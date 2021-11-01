package com.asiainfo.datacenter.mail;


/**
 * 邮件主体
 * @author shenluguo
 *
 */
public class SimpleMail {
	
	//发件人
	private String sender;
	//发件人密码
	private String password;
	//收件人,防止重复
	private String[] recivers;
	//主题
	private String subject;
	//内容
	private String content;
	//smtp服务器
	private String smtpHost;
	
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	public String[] getRecivers() {
		return recivers;
	}
	public void setRecivers(String[] recivers) {
		this.recivers = recivers;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content+"<br/><br/>from "+ ServerUtils.getLocalIp();
	}
	public String getSmtpHost() {
		return smtpHost;
	}
	public void setSmtpHost(String smtpHost) {
		this.smtpHost = smtpHost;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
}
