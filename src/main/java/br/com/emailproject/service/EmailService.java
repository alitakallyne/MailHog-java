package br.com.emailproject.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.ejb.Stateless;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.transaction.Transaction;

import org.jboss.resteasy.logging.impl.Log4jLogger;

import br.com.emailproject.model.Email;
import br.com.emailproject.util.LogUtil;

@Stateless
public class EmailService extends Thread {
	
	private List<Email> emails;
	private static final String HEADER_CONTEXT = "text/html; charset=utf-8";

	public void enviar(Email email) {
		emails = new ArrayList<>();
		emails.add(email);
		sendEmail();
	}
	
	public void enviar (List<Email>emails) {
		
		this.emails = emails;
		sendEmail();
	}

	private EmailService copy() {
		EmailService emailService = new EmailService();
		emailService.emails = emails;
		return emailService;
	}
	
	private void sendEmail() {
	  new Thread(this.copy()).start();
	  
	}
	@Override
	public void run() {
		
		Properties props = new Properties();
		
		props.put("email.smtp.starttls.enable", true);
		props.put("email.smtp.host",System.getProperty("email-project.email.smtp.host"));
		props.put("email.smtp.port",System.getProperty("email-project.email.smtp.port"));
		
		Session session = Session.getInstance(props);
		session.setDebug(false);
		
		for (Email email : emails) {
			
			try {
				Message message = new MimeMessage(session);
				message.setFrom(new InternetAddress(System.getProperty("email-project.email.from")));
				
				if(email.getDestinatario().contains("/")) {
					List<InternetAddress> emailLocal = new ArrayList<>();
					for (String e : email.getDestinatario().split("/")) {
						emailLocal.add(new InternetAddress(e));
					}
					
					message.addRecipients(Message.RecipientType.TO, emailLocal.toArray(new InternetAddress[0]));
				}else {
					InternetAddress para = new InternetAddress(email.getDestinatario());
					message.addRecipient(Message.RecipientType.TO, para);
				}
				
				message.setSubject(email.getAssunto());
				MimeBodyPart textPart = new MimeBodyPart();
				textPart.setHeader("Context-Type", HEADER_CONTEXT);
				textPart.setContent(email.getTexto(),HEADER_CONTEXT);
				Multipart mp = new MimeMultipart();
				mp.addBodyPart(textPart);
				message.setContent(mp);
				Transport.send(message);
			} catch (MessagingException e) {
				LogUtil.getLog(EmailService.class).error("Erro ao enviar email : "+e.getMessage());
			}
		}
	}

}
