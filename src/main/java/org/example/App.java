package org.example;

public class App 
{
    public static void main(String[] args) {
        String pdfPathOKTA = "D:/OKTA bernstein-1.pdf";
        String pdfPathWWD = "D:/WWD deutsche.pdf";
        PdfParser pdfParser = new PdfParser();
        try {
            pdfParser.parsePdf(pdfPathOKTA);
            pdfParser.parsePdf(pdfPathWWD);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
