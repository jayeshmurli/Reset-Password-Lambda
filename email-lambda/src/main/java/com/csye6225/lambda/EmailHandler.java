package com.csye6225.lambda;

import java.util.UUID;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class EmailHandler implements RequestHandler<String, String> {
	static final String FROM = "no-reply@csye6225-spring2019-" + System.getenv("AWS_DOMAIN_NAME") + ".me";

	static final String CONFIGSET = "ConfigSet";

	static final String SUBJECT = "Password reset for CSYE6225-SPRING2019-dandekars.me";

	static final String TEXTBODY = "This email was sent through Amazon SES " + "using the AWS SDK for Java.";

	public String handleRequest(String input, Context context) {
		String token;
		AmazonDynamoDB dbclient = AmazonDynamoDBClientBuilder.defaultClient();
		DynamoDB dynamoDb = new DynamoDB(dbclient);

		Table table = dynamoDb.getTable("csye6225");

		Item item = table.getItem("email", input);

		if (null != item) {
			token = item.getString("token");
		} else {
			token = UUID.randomUUID().toString();
			item = new Item().withPrimaryKey("email", input).withString("token", token);
			table.putItem(item);
		}

		String htmlBody = "Click on the following link to reset your password <br /> "
				+ "<a href = '#'>http://csye6225-spring2019-dandekars.me/reset?email=" + input + "&token=" + token
				+ "</a>";

		context.getLogger().log("Input: " + input);
		AmazonSimpleEmailService emailClient = AmazonSimpleEmailServiceClientBuilder.defaultClient();
		SendEmailRequest request = new SendEmailRequest().withDestination(new Destination().withToAddresses(input))
				.withMessage(new Message()
						.withBody(new Body().withHtml(new Content().withCharset("UTF-8").withData(htmlBody))
								.withText(new Content().withCharset("UTF-8").withData(TEXTBODY)))
						.withSubject(new Content().withCharset("UTF-8").withData(SUBJECT)))
				.withSource(FROM);
		emailClient.sendEmail(request);
		return token;
	}

}
