package com.asiainfo.datacenter.mail;

/**
 * Created by 董建斌 on 2018/9/26.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import java.util.Properties;


/**
 * 邮件发送服务
 * 
 * @author shenluguo
 *
 */
public class MailSender {
	
	private static Logger logger = LoggerFactory.getLogger(MailSender.class);
	
	/**
	 * 发送邮件
	 * @param mail
	 */
	public static void send(SimpleMail mail){
		Properties props = new Properties();
		//props.put("mail.smtp.auth", "true");//是否启用验证
		// 验证
//		MailAuthenticator authenticator = new MailAuthenticator(mail.getSender(), mail.getPassword());
	    props.put("mail.smtp.host", mail.getSmtpHost());
	    // 创建session
 		//getInstance(Properties props),Get a new Session object.
 		//getDefaultInstance(Properties props),Get the default Session object.
	    Session session = Session.getInstance(props, null);
	    // 创建mime类型邮件
 		final MimeMessage message = new MimeMessage(session);
 		// 设置发信人
 		try {
			message.setFrom(new InternetAddress(mail.getSender()));
			// 设置收件人们
	 		String[] recipients = mail.getRecivers();
	 		int num = recipients.length;
	 		InternetAddress[] addresses = new InternetAddress[num];
	 		for (int i = 0; i < num; i++) {
	 			addresses[i] = new InternetAddress(recipients[i]);
	 		}
	 		message.setRecipients(RecipientType.TO, addresses);
	 		// 设置主题
	 		message.setSubject(mail.getSubject());
	 		// 设置邮件内容
	 		message.setContent(mail.getContent(), "text/html;charset=utf-8");
	 		// 发送
	 		Transport.send(message);
		} catch (AddressException e) {
			e.printStackTrace();
			logger.error("", e);
		} catch (MessagingException e) {
			e.printStackTrace();
			logger.error("", e);
		}
	}
	
}

class MailAuthenticator extends Authenticator {
	/**
	 * 用户名（登录邮箱）
	 */
	private String username;
	/**
	 * 密码
	 */
	private String password;

	/**
	 * 初始化邮箱和密码
	 * 
	 * @param username
	 *            邮箱
	 * @param password
	 *            密码
	 */
	public MailAuthenticator(String username, String password) {
		this.username = username;
		this.password = password;
	}

	String getPassword() {
		return password;
	}

	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(username, password);
	}

	String getUsername() {
		return username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}
