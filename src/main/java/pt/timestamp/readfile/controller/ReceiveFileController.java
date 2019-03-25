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

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Base64;

import com.qoppa.pdf.PDFException;
import com.qoppa.pdf.SigningInformation;
import com.qoppa.pdf.form.SignatureField;
import com.qoppa.pdfSecure.PDFSecure;

@RestController
@Slf4j
public class ReceiveFileController {

/**
     * Receive a Json request
     *
     * @param JsonRequest
     * @return the JsonResponse
     */
    @RequestMapping(value="/receiveFile", method = RequestMethod.POST)
    public ResponseEntity<JsonResponse> receiveFile(@RequestBody JsonRequest request) {
        
        try{

            byte[] decodedContent = Base64.getDecoder().decode(request.getContent());

            PDFSecure pdfDoc = new PDFSecure(new ByteArrayInputStream(decodedContent), null);

            // Load the keystore that contains the digital id to use in signing
            FileInputStream pkcs12Stream = new FileInputStream ("C:\\Users\\marco.emidio\\Desktop\\signature.pfx");
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(pkcs12Stream, "mypassword".toCharArray());
            pkcs12Stream.close();

            // Create signing information
            SigningInformation signInfo = new SigningInformation (store, "myalias", "mypassword");
            // Create signature field on the first page
            Rectangle2D signBounds = new Rectangle2D.Double (36, 36, 144, 48);
            SignatureField signField = pdfDoc.addSignatureField(0, "signature", signBounds);

            // Apply digital signature
            pdfDoc.signDocument(signField, signInfo);

            // Save the document
            pdfDoc.saveDocument ("C:\\Users\\marco.emidio\\Desktop\\output.pdf");
            
            ByteArrayOutputStream signedContent = new  ByteArrayOutputStream();

            pdfDoc.saveDocument (signedContent);

            String encodedSignedContent = Base64.getEncoder().encodeToString(signedContent.toByteArray());

            log.info(request.getFileName());

            return new ResponseEntity<>(new JsonResponse(request.getFileName(), encodedSignedContent, "sucess"), HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new JsonResponse(request.getFileName(), "null", "exception"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


}