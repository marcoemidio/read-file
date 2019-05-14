package pt.timestamp.readfile.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.KeyStore.PasswordProtection;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import pt.timestamp.readfile.schema.JsonRequest;
import pt.timestamp.readfile.schema.JsonResponse;
import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.InMemoryDocument;
import eu.europa.esig.dss.SignatureLevel;
import eu.europa.esig.dss.SignatureValue;
import eu.europa.esig.dss.ToBeSigned;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.token.KSPrivateKeyEntry;
import eu.europa.esig.dss.token.JKSSignatureToken;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;

@RestController
@Slf4j
public class ReceiveFileEuropaDSS {

    // Secret values are updated only if spring boot application is restarted
    // https://github.com/Microsoft/azure-spring-boot/issues/447
    @Value("${CertificateAlias}")
    private String myAlias;
    
    @Value("${CertificatePassword}")
    private String pwd;

    @Value("${privatekey.storetype}")
    String keyStoreType;

    @Value("${signature-pkcs12}")
    byte[] certificate;

    /**
     * Receive a PDF document and signs it using Europa DSS eSignature
     *
     * @param JsonRequest
     * @return the JsonResponse
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws UnrecoverableKeyException
     */
    @RequestMapping(value = "/receiveFileDss", method = RequestMethod.POST)
    public ResponseEntity<JsonResponse> receiveFile(@RequestBody JsonRequest request) throws UnrecoverableKeyException,
            KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        log.info("entering");
        // Preparing parameters for the PAdES signature
        PAdESSignatureParameters parameters = new PAdESSignatureParameters();
        // We choose the level of the signature (-B, -T, -LT, -LTA).
        parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
        // We set the digest algorithm to use with the signature algorithm. You must use the
        // same parameter when you invoke the method sign on the token. The default value is
        // SHA256
        parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
        
        KSPrivateKeyEntry privateKey = getKeyStore(System.getenv("KEYSTORE_PATH"), keyStoreType, pwd.toCharArray());
        // We set the signing certificate
        parameters.setSigningCertificate(privateKey.getCertificate());
        // We set the certificate chain
        parameters.setCertificateChain(privateKey.getCertificateChain());
        // Create common certificate verifier
        CommonCertificateVerifier commonCertificateVerifier = new CommonCertificateVerifier();
        // Create PAdESService for signature
        PAdESService service = new PAdESService(commonCertificateVerifier);
        
        byte[] decodedContent = Base64.getDecoder().decode(request.getContent());

        DSSDocument toSignDocument = new InMemoryDocument(decodedContent);
        // Get the SignedInfo segment that need to be signed.
        ToBeSigned dataToSign = service.getDataToSign(toSignDocument, parameters);
        // This function obtains the signature value for signed information using the
        // private key and specified algorithm
        DigestAlgorithm digestAlgorithm = parameters.getDigestAlgorithm();

        JKSSignatureToken signingToken = new JKSSignatureToken(System.getenv("KEYSTORE_PATH"), new PasswordProtection(pwd.toCharArray()));

        SignatureValue signatureValue = signingToken.sign(dataToSign, digestAlgorithm, privateKey);
        signingToken.close();

        // We invoke the xadesService to sign the document with the signature value obtained in the previous step.
        DSSDocument signedDocument = service.signDocument(toSignDocument, parameters, signatureValue);

        // Writes signed DSSDocument to OutputStream 
        ByteArrayOutputStream signedContent = new  ByteArrayOutputStream();
        signedDocument.writeTo(signedContent);

        // Base64Encode signed OutputStream
        String encodedSignedContent = Base64.getEncoder().encodeToString(signedContent.toByteArray());
        log.info("leaving");
        return new ResponseEntity<>(new JsonResponse(request.getFileName(), encodedSignedContent, "sucess"), HttpStatus.CREATED);
    }


    private KSPrivateKeyEntry getKeyStore(String file, String type, char[] pwd) 
        throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        
        log.info("azure loaded secrets: pwd: '{}' alias: '{}' byte: '{}'", pwd, myAlias, certificate);

        KeyStore keyStore = KeyStore.getInstance(type);
        
        // Load the keystore that contains the digital id to use in signing
        //try (FileInputStream keyStoreStream = new FileInputStream (file)){    
        try (InputStream keyStoreStream = new ByteArrayInputStream(certificate)){    
            keyStore.load(keyStoreStream, new char[0]);
        }
        
        Key key = keyStore.getKey(myAlias, pwd);
        Certificate cert = keyStore.getCertificate(myAlias);
        KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry((PrivateKey) key, new Certificate[]{cert});
        
        return new KSPrivateKeyEntry(myAlias, privateKeyEntry);
    }
}