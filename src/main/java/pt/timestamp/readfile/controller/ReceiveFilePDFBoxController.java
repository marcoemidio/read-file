package pt.timestamp.readfile.controller;

import lombok.extern.slf4j.Slf4j;
import pt.timestamp.readfile.schema.JsonRequest;
import pt.timestamp.readfile.schema.JsonResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Base64;

import org.apache.pdfbox.examples.signature.CreateSignature;
import org.apache.pdfbox.pdmodel.PDDocument;

@RestController
@Slf4j
public class ReceiveFilePDFBoxController {

    public boolean externallySign;

    /**
     * Receive a Json request
     *
     * @param JsonRequest
     * @return the JsonResponse
     */
    @RequestMapping(value="/receiveFilePDFBox", method = RequestMethod.POST)
    public ResponseEntity<JsonResponse> receiveFilePDFBox(@RequestBody JsonRequest request) {
        
        try{

            byte[] decodedContent = Base64.getDecoder().decode(request.getContent());

            // Load the keystore that contains the digital id to use in signing
            FileInputStream pkcs12Stream = new FileInputStream ("C:\\Users\\marco.emidio\\Desktop\\signature.pfx");
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(pkcs12Stream, "mypassword".toCharArray());
            pkcs12Stream.close();

            // sign PDF
            CreateSignature signing = new CreateSignature(keystore, "mypassword".toCharArray());
            externallySign = false;
            signing.setExternalSigning(externallySign);
            
            ByteArrayOutputStream signedContent = new  ByteArrayOutputStream();

            PDDocument pdf = PDDocument.load(decodedContent);

            signing.signDetached(pdf, signedContent);

            log.info(request.getFileName());

            String encodedSignedContent = Base64.getEncoder().encodeToString(signedContent.toByteArray());

            return new ResponseEntity<>(new JsonResponse(request.getFileName(), encodedSignedContent, "sucess"), HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new JsonResponse(request.getFileName(), "null", "exception"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}