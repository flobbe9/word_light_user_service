package de.word_light.user_service.services;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import de.word_light.user_service.exception.ApiException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.NotBlank;


@Service
public class MailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.sender.email}")
    private String senderEmail;


    /**
     * Simple mail sending method. Uses 'spring.mail.username' property as sender mail.
     * 
     * @param to reciever email address
     * @param subject of email
     * @param text content of email
     * @param html true if {@code text} is written as HTML, else false
     * @param inlines map of <contentId, file>. {@code contentId} has to be referenced in the html document like this: 
     *               {@code <img src="cid:contentId" />}
     * @param attachments files to attach to email, optional
     */
    public void sendMail(@NotBlank(message = "'to' cannot be blank or null") String to, 
                         @NotBlank(message = "'subject' cannot be blank or null") String subject, 
                         @NotBlank(message = "'text' cannot be blank or null") String text, 
                         boolean html, 
                         Map<String, File> inlines,
                         File... attachments) {

        try {
            MimeMessage mimeMessage = createMimeMessage(to, subject, text, html, inlines, attachments);

            javaMailSender.send(mimeMessage);

        } catch (MailException | MessagingException e) {
            throw new ApiException("Failed to send mail.", e);
        }
    }


    /**
     * Create simple {@code mimeMessage} for mail sending method.
     * 
     * @param to reciever email address
     * @param subject of email
     * @param text content of email
     * @param html true if 'text' is written as HTML, else false
     * @param attachments files to attach to email
     * @param inlines map of <contentId, file>. {@code contentId} has to be referenced in the html document like this: 
     *               {@code <img src="cid:contentId" />}
     * @return simple mimeMessage with given attributes
     * @throws MessagingException
     */
    private MimeMessage createMimeMessage(String to, 
                                          String subject, 
                                          String text, 
                                          boolean html, 
                                          Map<String, File> inlines,
                                          File... attachments) throws MessagingException {

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED);

        helper.addTo(to);
        helper.setFrom(this.senderEmail);
        helper.setSubject(subject);
        helper.setText(text, html);

        if (inlines != null)
            addInlines(helper, inlines);

        if (attachments != null)
            addAttatchments(helper, attachments);
        
        return mimeMessage;
    }


    private void addAttatchments(MimeMessageHelper helper, File[] attachments) throws MessagingException {

        for (File file : attachments) 
            helper.addAttachment(file.getName(), file);
    }


    private void addInlines(MimeMessageHelper helper, Map<String, File> inlines) throws MessagingException {

        for (Entry<String, File> entry : inlines.entrySet()) {
            String contentId = entry.getKey();
            File file = entry.getValue();

            helper.addInline(contentId, file);
        };
    }
}