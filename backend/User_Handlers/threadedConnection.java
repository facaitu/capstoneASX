package com.amazonaws.samples;

/************************************************************************
* This class handles the individual connections, and responds to their	*
* requests with the appropriate data, or where applicable, error code	*
************************************************************************/

import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import java.util.zip.InflaterInputStream;
import java.util.zip.DeflaterOutputStream;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class threadedConnection implements Runnable
{
	private Socket client;
	private static String date;
	private static String time;
	private static String  fileName;
	private AWSCredentials credentials;
	private static AmazonS3 s3Client;
	private static final String bucket = "asx-user-store";
	private static String AccessKey = creds.accessKey;
	private static String SecretKey = creds.secretAccessKey;
	private static String fileData;
	
	public threadedConnection(Socket cSocket)
	{
		this.client = cSocket;
		this.credentials = new BasicAWSCredentials(threadedConnection.AccessKey,threadedConnection.SecretKey);
		threadedConnection.s3Client  = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(Regions.AP_SOUTHEAST_2).build();
		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		DateFormat tf = new SimpleDateFormat("HHmmss");
		df.setTimeZone(TimeZone.getTimeZone("Australia/Victoria"));
		tf.setTimeZone(TimeZone.getTimeZone("Australia/Victoria"));
		Date dateobj = new Date();
		threadedConnection.date = df.format(dateobj);
		threadedConnection.time = tf.format(dateobj);
		String directory = "serverLogs/" + date + "/";
		new File(directory).mkdirs();
		threadedConnection.fileName = "serverLogs/" + date + "/" + time + ".log";
	}
	
	public void run()
	{
		BufferedReader connectionRead = null;
		DeflaterOutputStream deflStream = null;
		BufferedWriter fileWrite = null;
		fileData = "";
		String line = null;
		byte[] bytes = null;
		try
		{
			fileWrite = new BufferedWriter(new FileWriter(threadedConnection.fileName));
			//System.out.println("[" + this.client.getRemoteSocketAddress() + "] New incoming connection");
			fileData += "New incoming connection [" + this.client.getRemoteSocketAddress() + "] \n";
			connectionRead = new BufferedReader(new InputStreamReader(new InflaterInputStream(this.client.getInputStream())));
			line = connectionRead.readLine();
			if(line != null)
			{
				deflStream = new DeflaterOutputStream(this.client.getOutputStream(), true);
				//System.out.println("[" + this.client.getRemoteSocketAddress() + "] Requesting: " + line);
				if(line.equals("login"))
				{
					String userEmail = connectionRead.readLine();
					String passwdHash = connectionRead.readLine();
					fileData += "Requesting: " + line + "; Email Hash: " + userEmail + "; Password Hash: " + passwdHash + "\n";
					if((line = login(userEmail,passwdHash)) != null)
					{
						//System.out.println("[" + this.client.getRemoteSocketAddress() + "] Returning: Valid Login Data");
						fileData += "Returning: Valid Login Data\n";
						fileData += line + "\n";
						bytes = line.getBytes("UTF-8");
					}
					else
					{
						//System.out.println("[" + this.client.getRemoteSocketAddress() + "] Returning: 401");
						fileData += "Returning: 401\n";
						bytes = "401".getBytes("UTF-8");
					}
				}
				else if(line.equals("history"))
				{
					String emailHash = connectionRead.readLine();
					String type = connectionRead.readLine();
					fileData += "Requesting: " + line + "; Email Hash: " + emailHash + "; Type: " + type + "\n";
					if((line = getHistory(emailHash,type)) != null)
					{
						//System.out.println("[" + this.client.getRemoteSocketAddress() + "] Returning: Valid History Data");
						fileData += "Returning: Valid History Data\n";
						fileData += line + "\n";
						bytes = line.getBytes("UTF-8");
					}
					else
					{
						//System.out.println("[" + this.client.getRemoteSocketAddress() + "] Returning: 500");
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("register"))
				{
					String newPasswdHash = connectionRead.readLine();
					String newFName = connectionRead.readLine();
					String newSName = connectionRead.readLine();
					String newUEmail = connectionRead.readLine();
					fileData += "Requesting: " + line + "; Password Hash: " + newPasswdHash + "; First Name: " + newFName + "; Surname: " + newSName + "; Email: " + newUEmail +"\n";
					if(register(newPasswdHash, newFName, newSName, newUEmail))
					{
						//System.out.println("[" + this.client.getRemoteSocketAddress() + "] Returning: 200");
						fileData += "Returning: 200\n";
						bytes = "200".getBytes("UTF-8");
					}
					else
					{
						//System.out.println("[" + this.client.getRemoteSocketAddress() + "] Returning: 500");
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("save"))
				{
					String userEmail = connectionRead.readLine();
					String userJson = connectionRead.readLine();
					String userTransaction = connectionRead.readLine();
					fileData += "Requesting: " + line + "; Email Hash: " + userEmail + "\n JSON String: " + userJson + "\n Transaction String: " + userTransaction + "\n";
					if(save(userEmail, userJson, userTransaction))
					{
						//System.out.println("[" + this.client.getRemoteSocketAddress() + "] Returning: 200");
						fileData += "Returning: 200\n";
						bytes = "200".getBytes("UTF-8");
					}
					else
					{
						//System.out.println("[" + this.client.getRemoteSocketAddress() + "] Returning: 500");
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("leaders"))
				{
					int topPos = Integer.parseInt(connectionRead.readLine());
					int count = Integer.parseInt(connectionRead.readLine());
					fileData += "Requesting: " + line + "; Top Position: " + topPos + "; Num: " + count +  "\n";
					String leaders = "";
					if((leaders = leaderboard(topPos, count)) != null)
					{
						//System.out.println("[" + this.client.getRemoteSocketAddress() + "] Returning: Leaderboard Data");
						fileData += "Returning: Leaderboard Data\n";
						fileData += leaders + "\n";
						bytes = leaders.getBytes("UTF-8");
					}
					else
					{
						//System.out.println("[" + this.client.getRemoteSocketAddress() + "] Returning: 500");
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("getUser"))
				{
					String emailHash = connectionRead.readLine();
					fileData += "Requesting: " + line + "; Email Hash: " + emailHash + "\n";
					String response = "";
					if((response = getUsers(emailHash)) != null)
					{
						//System.out.println("[" + this.client.getRemoteSocketAddress() + "] Returning: User Data");
						fileData += "Returning: User Data\n";
						fileData += response + "\n";
						bytes = response.getBytes("UTF-8");
					}
					else
					{
						//System.out.println("[" + this.client.getRemoteSocketAddress() + "] Returning: 500");
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("stockHistory"))
				{
					String stockCode = connectionRead.readLine();
					int startDate = Integer.parseInt(connectionRead.readLine());
					int endDate = Integer.parseInt(connectionRead.readLine());
					fileData += "Requesting: " + line + "; ASX Code: " + stockCode + "; Start Date: " + startDate + "; End Date: " + endDate + "\n";
					String response = "";
					if((response = stockHistory(stockCode, startDate, endDate)) != null)
					{
						response = "200\n" + response;
						fileData += "Returning: Stock History Data\n";
						fileData += response + "\n";
						bytes = response.getBytes("UTF-8");
					}
					else
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("setBuy"))
				{
					try
					{
						double flat = Double.parseDouble(connectionRead.readLine());
						double percentage = Double.parseDouble(connectionRead.readLine());
						fileData += "Requesting: " + line + "; Flat: " + flat + "; Percentage: " + percentage + "\n";
						UserServer.flatBuyFee = flat;
						UserServer.perBuyFee = percentage;
						fileData += "Returning: 200\n";
						bytes = "200".getBytes("UTF-8");
					}
					catch (NumberFormatException nfe)
					{
						fileData += "Returning: 400\n";
						bytes = "400".getBytes("UTF-8");
					}
				}
				else if(line.equals("setSell"))
				{
					try
					{
						double flat = Double.parseDouble(connectionRead.readLine());
						double percentage = Double.parseDouble(connectionRead.readLine());
						fileData += "Requesting: " + line + "; Flat: " + flat + "; Percentage: " + percentage + "\n";
						UserServer.flatSellFee = flat;
						UserServer.perSellFee = percentage;
						fileData += "Returning: 200\n";
						bytes = "200".getBytes("UTF-8");
					}
					catch (NumberFormatException nfe)
					{
						fileData += "Returning: 400\n";
						bytes = "400".getBytes("UTF-8");
					}
				}
				else if(line.equals("getBuy"))
				{
					try
					{
						fileData += "Requesting: " + line + "\n";
						String response = "200\n" + UserServer.flatBuyFee + "\n" + UserServer.perBuyFee + "\n";
						fileData += "Returning: " + response;
						bytes = response.getBytes("UTF-8");
					}
					catch (NumberFormatException nfe)
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("getSell"))
				{
					try
					{
						fileData += "Requesting: " + line + "\n";
						String response = "200\n" + UserServer.flatSellFee + "\n" + UserServer.perSellFee + "\n";
						fileData += "Returning: " + response;
						bytes = response.getBytes("UTF-8");
					}
					catch (NumberFormatException nfe)
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("sendMessage"))
				{
					String sender = connectionRead.readLine();
					String recipient = connectionRead.readLine();
					String subject = connectionRead.readLine();
					String contents = "";
					while((line = connectionRead.readLine()) != null)
					{
						contents += line + "\n";
					}
					fileData += "Requesting: sendMessage; Sender: " + sender + "; Recipient: " + recipient + "; Subject: " + subject + "\nContents: " + contents + "\n";
					if(sendMessage(sender, recipient, subject, contents))
					{
						fileData += "Returning: 200\n";
						bytes = "200".getBytes("UTF-8");
					}
					else
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("getMessageList"))
				{
					String user = connectionRead.readLine();
					fileData += "Requesting: " + line + "; User: " + user + "\n";
					String gmlReturn = getMessageList(user, "message");
					if(gmlReturn != null)
					{
						if(gmlReturn.equals("0"))
						{
							fileData += "Returning: 204\n";
							bytes = "204".getBytes("UTF-8");
						}
						else
						{
							fileData += "Returning: " + gmlReturn + "\n";
							bytes = gmlReturn.getBytes("UTF-8");
						}
					}
					else
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("getMessage"))
				{
					String user = connectionRead.readLine();
					int mailID = Integer.parseInt(connectionRead.readLine());
					fileData += "Requesting: " + line + "; User: " + user + "; Mail ID: " + mailID + "\n";
					String message = getMessage(user, mailID);
					if(message != null)
					{
						fileData += "Returning: " + message + "\n";
						bytes = message.getBytes("UTF-8");
					}
					else
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("getDeleted"))
				{
					String user = connectionRead.readLine();
					fileData += "Requesting: " + line + "; User: " + user + "\n";
					String gmlReturn = getDeletedMessageList(user, "message");
					if(gmlReturn != null)
					{
						if(gmlReturn.equals("0"))
						{
							fileData += "Returning: 204\n";
							bytes = "204".getBytes("UTF-8");
						}
						else
						{
							fileData += "Returning: " + gmlReturn + "\n";
							bytes = gmlReturn.getBytes("UTF-8");
						}
					}
					else
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("deleteMessage"))
				{
					String user = connectionRead.readLine();
					int mailID = Integer.parseInt(connectionRead.readLine());
					fileData += "Requesting: " + line + "; User: " + user + "; Mail ID: " + mailID + "\n";
					if(deleteMessage(user, mailID))
					{
						fileData += "Returning: 200\n";
						bytes = "200".getBytes("UTF-8");
					}
					else
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("unreadMail"))
				{
					String user = connectionRead.readLine();
					fileData += "Requesting: " + line + "; User: " + user + "\n";
					if((line = getUnreadMail(user)) != null)
					{
						fileData += "Returning: " + line + "\n";
						bytes = line.getBytes("UTF-8");
					}
					else
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("markRead"))
				{
					String user = connectionRead.readLine();
					int mailID = Integer.parseInt(connectionRead.readLine());
					fileData += "Requesting: " + line + "; User: " + user + "; mailID: " + mailID +"\n";
					if(markMailRead(user, mailID))
					{
						fileData += "Returning: 200\n";
						bytes = "200".getBytes("UTF-8");
					}
					else
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("markUnread"))
				{
					String user = connectionRead.readLine();
					int mailID = Integer.parseInt(connectionRead.readLine());
					fileData += "Requesting: " + line + "; User: " + user + "; mailID: " + mailID +"\n";
					if(markMailUnread(user, mailID))
					{
						fileData += "Returning: 200\n";
						bytes = "200".getBytes("UTF-8");
					}
					else
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("getID"))
				{
					String user = connectionRead.readLine();
					fileData += "Requesting: " + line + "; User: " + user + "\n";
					String ID = "";
					if((ID = userHashToID(user, "mainThread")) != null)
					{
						fileData += "Returning: " + ID + "\n";
						bytes = ID.getBytes("UTF-8");
					}
					else
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("deleteAccount"))
				{
					String user = connectionRead.readLine();
					fileData += "Requesting: " + line + "; User: " + user + "\n";
					if(deleteAccount(user))
					{
						fileData += "Returning: 200\n";
						bytes = "200".getBytes("UTF-8");
					}
					else
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("sendFunds"))
				{
					String sender = connectionRead.readLine();
					String recipient = connectionRead.readLine();
					float amount = Float.parseFloat(connectionRead.readLine());
					fileData += "Requesting: " + line + "; Sender: " + sender + "; Recipient: " + recipient + "; Amount: " + amount + "\n";
					if(sendFunds(sender, recipient, amount))
					{
						fileData += "Returning: 200\n";
						bytes = "200".getBytes("UTF-8");
					}
					else
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("acceptFunds"))
				{
					String user = connectionRead.readLine();
					String fundID = connectionRead.readLine();
					float amount = Float.parseFloat(connectionRead.readLine());
					fileData += "Requesting: " + line + "; User: " + user + "; Fund ID: " + fundID + "; Amount: " + amount + "\n";
					if(acceptFunds(user, fundID, amount))
					{
						fileData += "Returning: 200\n";
						bytes = "200".getBytes("UTF-8");
					}
					else
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else if(line.equals("getFundsList"))
				{
					String user = connectionRead.readLine();
					fileData += "Requesting: " + line + "; User: " + user + "\n";
					String gmlReturn = getMessageList(user, "funds");
					if(gmlReturn != null)
					{
						if(gmlReturn.equals("0"))
						{
							fileData += "Returning: 204\n";
							bytes = "204".getBytes("UTF-8");
						}
						else
						{
							fileData += "Returning: " + gmlReturn + "\n";
							bytes = gmlReturn.getBytes("UTF-8");
						}
					}
					else
					{
						fileData += "Returning: 500\n";
						bytes = "500".getBytes("UTF-8");
					}
				}
				else
				{
					while((line = connectionRead.readLine()) != null)
					{
						line += line + "/n";
					}
					fileData  += "Requested: " + line + "\n";
					fileData  += "Returning: 400: BAD REQUEST!\n";
					bytes = "400: BAD REQUEST!".getBytes("UTF-8");
				}
				deflStream.write(bytes);
				deflStream.finish();
				deflStream.flush();
				deflStream.close();
				this.client.close();
				//System.out.println("[" + this.client.getRemoteSocketAddress() + "] End connection");
				fileData += "End connection [" + this.client.getRemoteSocketAddress() + "] \n";
				return;
			}
		}
		catch (SocketException se)
		{
			System.out.println("[" + this.client.getRemoteSocketAddress() + "] Exception while performing socket operation: " + se);
			se.printStackTrace();
			fileData += "Exception while performing socket operation: " + se + "\n";
			fileData += se.getStackTrace() + "\n";
		}
		catch(IOException e)
		{
			System.out.println("[" + this.client.getRemoteSocketAddress() + "] Exception while opening server socket: " + e);
			e.printStackTrace();
			fileData += "Exception while opening server socket: " + e + "\n";
			fileData += e.getStackTrace() + "\n";
		}
		catch (NumberFormatException ex)
		{
			System.out.println("[" + this.client.getRemoteSocketAddress() + "] Exception when converting String to Int: " + ex);
			ex.printStackTrace();
			fileData += "Exception when converting String to Int: " + ex + "\n";
			fileData += ex.getStackTrace() + "\n";
		}
		finally
		{
			try
			{
				deflStream.close();
				this.client.close();
				fileWrite.write(fileData);
				fileWrite.close();
			}
			catch (IOException e)
			{
				System.out.println("[" + this.client.getRemoteSocketAddress() + "] Exception while closing sockets: " + e);
				e.printStackTrace();
				fileData += "Exception while closing sockets: " + e + "\n";
			}
		}
		return;
	}
	
	private static boolean saveToS3(String S3bucket, String filePath, String contents, String originFunction)
	{
		try
		{
			byte[] contentAsBytes = contents.getBytes("UTF-8");
			ByteArrayInputStream contentAsStream = new ByteArrayInputStream(contentAsBytes);
			ObjectMetadata md = new ObjectMetadata();
			md.setContentLength(contentAsBytes.length);
			s3Client.putObject(new PutObjectRequest(S3bucket, filePath, contentAsStream, md));
			contentAsStream.close();
			return true;
		}
		catch (AmazonServiceException ase)
		{
            System.out.println("Caught an AmazonServiceException");
            ase.printStackTrace();
            fileData += originFunction + ": Caught an AmazonServiceException: " + ase + "\n";
            return false;
		}
		catch (AmazonClientException ace)
		{
            System.out.println("Caught an AmazonClientException: " + ace);
            ace.printStackTrace();
            fileData += originFunction + ": Caught an AmazonClientException: " + ace + "\n";
            return false;
		}
		catch (UnsupportedEncodingException e)
		{
			System.out.println("Caught an UnsupportedEncodingException");
			e.printStackTrace();
			fileData += originFunction + ": Caught an UnsupportedEncodingException: " + e + "\n";
			return false;
		}
		catch (IOException e)
		{
			System.out.println("Exception whe trying to close stream");
			e.printStackTrace();
			fileData += originFunction + ": Exception whe trying to close stream: " + e + "\n";
			return false;
		}
		catch (NumberFormatException ex)
		{
			System.out.println("Exception when converting String to Int: " + ex);
			ex.printStackTrace();
			fileData += originFunction + ": Exception when converting String to Int: " + ex + "\n";
			return false;
		}
	}
	
	private static String readFromS3(String S3bucket, String filePath, String originFunction)
	{
		S3Object object = s3Client.getObject(new GetObjectRequest(S3bucket, filePath));
		InputStream objectData = object.getObjectContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(objectData));
		String outputData = "";
		String line = "";
		try
		{
			while((line = reader.readLine()) != null)
			{
				outputData += line + "\n";
			}
			return outputData;
		}
		catch (AmazonServiceException ase)
		{
            System.out.println("Caught an AmazonServiceException");
            ase.printStackTrace();
            fileData += originFunction + ": Caught an AmazonServiceException: " + ase + "\n";
            return null;
		}
		catch (AmazonClientException ace)
		{
            System.out.println("Caught an AmazonClientException: " + ace);
            ace.printStackTrace();
            fileData += originFunction + ": Caught an AmazonClientException: " + ace + "\n";
            return null;
		}
		catch (UnsupportedEncodingException e)
		{
			System.out.println("Caught an UnsupportedEncodingException");
			e.printStackTrace();
			fileData += originFunction + ": Caught an UnsupportedEncodingException: " + e + "\n";
			return null;
		}
		catch (IOException e)
		{
			System.out.println("Exception whe trying to close stream");
			e.printStackTrace();
			fileData += originFunction + ": Exception whe trying to close stream: " + e + "\n";
			return null;
		}
		catch (NumberFormatException ex)
		{
			System.out.println("Exception when converting String to Int: " + ex);
			ex.printStackTrace();
			fileData += originFunction + ": Exception when converting String to Int: " + ex + "\n";
			return null;
		}
	}
	
	private static ArrayList<String> getList(String bucket, String prefix)
	{
		ArrayList<String> returnList = new ArrayList<String>();
		ObjectListing objectList = s3Client.listObjects(bucket, prefix);
		List<S3ObjectSummary> summaries = objectList.getObjectSummaries();
		if(summaries.size() != 0)
		{
			if(objectList.isTruncated())
			{
				do
				{
					for(S3ObjectSummary summary : summaries)
					{
						String key = summary.getKey();
						returnList.add(key);
					}
					objectList = s3Client.listNextBatchOfObjects(objectList);
				}while (objectList.isTruncated());
			}
			else
			{
				for(S3ObjectSummary summary : summaries)
				{
					String key = summary.getKey();
					returnList.add(key);
				}
			}
		}
		return returnList;
	}
	
	private static String userHashToID(String emailHash, String originFunction)
	{
		//Grab user record into Buffered Reader Stream
		String userCreds = "creds/"+emailHash+".rec";
		if(!s3Client.doesObjectExist(bucket, userCreds))
		{
			System.out.println("User does not exist!");
			fileData += originFunction + ": User does not exist!\n";
			return null;
		}
		String storedCreds;
		if((storedCreds = readFromS3(bucket, userCreds, originFunction)).equals(null))
		{
			return null;
		}
		String userID = storedCreds.split("\n")[1];
		return userID;
	}
	
	private static String login(String emailHash, String passwdHash)
	{	
		//Define known variables
		String userCreds = "creds/"+emailHash+".rec";
		
		//Grab user record into Buffered Reader Stream
		if(!s3Client.doesObjectExist(bucket, userCreds))
		{
			System.out.println("User does not exist!");
			fileData += "login: User does not exist!\n";
			return null;
		}
		String storedCreds;
		if((storedCreds = readFromS3(bucket, userCreds, "login")).equals(null))
		{
			return null;
		}
		String hash = storedCreds.split("\n")[0];
		String userID = storedCreds.split("\n")[1];
		if(hash.equals(passwdHash))
		{
			String userData = "data/"+userID+"/data.json";
			String transHistory = "data/"+userID+"/purchaseHistory.json";
			String valueHistory = "data/"+userID+"/valueHistory.json";
			if(!s3Client.doesObjectExist(bucket, userData))
			{
				System.out.println("User data file does not exist!");
				fileData += "login: User data file does not exist!\n";
				return null;
			}
			
			//Get User Data
			String data;
			if((data = readFromS3(bucket, userData, "login")).equals(null))
			{
				return null;
			}
			
			//Get User Transaction History
			data += "transaction\n";
			String transaction;
			if((transaction = readFromS3(bucket, transHistory, "login")).equals(null))
			{
				return null;
			}
			data += transaction;
			
			//Get User Value History
			data += "value\n";
			String value;
			if((value = readFromS3(bucket, valueHistory, "login")).equals(null))
			{
				return null;
			}
			data += value;
			
			return data;
		}
		else
		{
			return null;
		}
	}
	
	private static String getHistory(String emailHash, String type)
	{
		//Define known variables
		String data = "";
		String file = "";
		
		/**Convert users emailHash to unique ID**/
		String userID;
		if((userID = userHashToID(emailHash, "getHistory")).equals(null))
		{
			return null;
		}
		
		if(type.equals("transaction"))
		{
			//Get user purchase history
			file = "data/"+userID+"/purchaseHistory.json";
		}
		else if(type.equals("value"))
		{
			//Get user value history
			file = "data/"+userID+"/valueHistory.json";
		}
		if((data = readFromS3(bucket, file, "getHistory")).equals(null))
		{
			return null;
		}
		return data;
	}
	
	private static boolean register(String passwdHash, String fname, String sName, String uEmail)
	{
		//Creates a new instance of userID.json and userID.rec for future use
		//Adds user to leader board with score 0
		String emailHash = Integer.toString(uEmail.hashCode());
		String userCreds = "creds/"+emailHash+".rec";
		
		//If user already exists, return false, otherwise create new user
		if(!s3Client.doesObjectExist(bucket, userCreds))
		{
			/**Genereate new users ID number**/
			int uID = 9999;
			int comp = 9999;
			//Get list of existing users
			String prefix = "data/";
			ArrayList<String> userList = getList(bucket, prefix);
			userList.remove(0);
			for(String user:userList)
			{
				comp = Integer.parseInt(user.split("[/.]")[1]);
				if(comp > uID)
				{
					uID = comp;
				}
			}
			//Generate new unique user ID
			uID++;
			String userID = Integer.toString(uID);
			
			//Define file paths for user files
			String userData = "data/"+userID+"/data.json";
			String purchaseHistory = "data/"+userID+"/purchaseHistory.json";
			String valueHistory = "data/"+userID+"/valueHistory.json";
			
			/**Creates users files**/
			//Create user credentials file
			String recordData = passwdHash + "\n" + userID;
			if(!saveToS3(bucket, userCreds, recordData, "register"))
			{
				return false;
			}
			
			//Create User JSON String
			JSONObject dataJSON = new JSONObject();
			dataJSON.put("Name", fname);
			dataJSON.put("Surname", sName);
			dataJSON.put("Email", uEmail);
			dataJSON.put("Balance", "1000000.00");
			dataJSON.put("Shares","");
			dataJSON.put("Score","0.00");
			dataJSON.put("Rights","trader");
			String dataString = dataJSON.toString();
			
			//Create History Strings
			String pHisString = "";
			String vHisString = "";
			
			//Write Data file
			if(!saveToS3(bucket, userData, dataString, "register"))
			{
				return false;
			}
			//Write purchaseHistory File
			if(!saveToS3(bucket, purchaseHistory, pHisString, "register"))
			{
				return false;
			}
			//Write valueHistory File
			if(!saveToS3(bucket, valueHistory, vHisString, "register"))
			{
				return false;
			}
			
			/**Add user to leader board**/
			String leaderboard = "leaderboard.csv";
			String userEntry = userID + ":0.00";
			//Read Current leaderboard and append the new user to the end of it
			String newLeaderboard = "";
			if((newLeaderboard = readFromS3(bucket, leaderboard, "register")) != null)
			{
				newLeaderboard += userEntry;
				if(!saveToS3(bucket, leaderboard, newLeaderboard, "register"))
				{
					return false;
				}
			}
			return true;
		}
		System.out.println("User already exists!");
		fileData += "register: User already exists!\n";
		return false;
	}
	
	private static boolean save(String emailHash, String newJSON, String transaction)
	{
		/**Convert users emailHash to unique ID**/
		/**Convert users emailHash to unique ID**/
		String userID;
		if((userID = userHashToID(emailHash, "save")).equals(null))
		{
			return false;
		}
		String userData = "data/"+userID+"/data.json";
		String dataString = newJSON + "\n";
		
		//Update Data file
		if(!saveToS3(bucket, userData, dataString, "save"))
		{
			return false;
		}
		
		/**Read transaction history file append new data to the end of it**/
		if(transaction != null)
		{
			String transactionHistory = "data/"+userID+"/purchaseHistory.json";
			//Read transaction history
			dataString = "";
			if((dataString = readFromS3(bucket, transactionHistory, "save")).equals(null))
			{
				return false;
			}
			//Append new history to end of read data
			dataString += transaction;
			//Upload new history to transaction history
			if(!saveToS3(bucket, transactionHistory, dataString, "save"))
			{
				return false;
			}
		}
		
		/**Update leaderboard**/
		String leaderboard = "leaderboard.csv";
		//Generate user leaderboard entry
		JSONObject data = new JSONObject(newJSON);
		String sScore = data.get("Score").toString();
		float fScore = Float.valueOf(sScore);
		String userEntry = userID + ":" + fScore + "\n";
		
		String fileContents = "";
		boolean written = false;
		//Read current leaderboard
		String leaders;
		if((leaders = readFromS3(bucket, leaderboard, "save")).equals(null))
		{
			return false;
		}
		ArrayList<String> leaderLines = new ArrayList<String>(Arrays.asList(leaders.split("\n")));
		for(String line:leaderLines)
		{
			String compUserID = line.split(":")[0];
			String compScoreString = line.split(":")[1];
			float compScore = Float.valueOf(compScoreString);
			if(!compUserID.equals(userID)) //If the current line does not belong to the updating user
			{
				if(written)
				{
					fileContents += line + "\n";
				}
				else if(fScore > compScore) //If users score is more than the current line being read
				{
					written = true;
					fileContents += userEntry + line + "\n";
				}
				else
				{
					fileContents += line + "\n";
				}
			}
		}
		if(!written) //reached end of file and player score is the lowest
		{
			fileContents = fileContents + userEntry;
		}
		
		//Write updated leaderboard to S3
		if(!saveToS3(bucket, leaderboard, fileContents, "save"))
		{
			return false;
		}
		return true;
	}
	
	private static String leaderboard(int topN, int count)
	{
		//Take the first topN entries from leaderboards.csv
		//If bottomN == 0, return entire leaderboard
		//Create a string to send back formatted as {'Name':'Name','sName':'sName','Score':'Score'},{},{},{} etc.
		//Return said string
		
		//Read leaderboard file line by line
		//Connect to S3
		String leaderboard = "leaderboard.csv";
		String fullLeaderboard;
		if((fullLeaderboard = readFromS3(bucket, leaderboard, "leaderboard")).equals(null))
		{
			return null;
		}
		ArrayList<String> leaderLines = new ArrayList<String>(Arrays.asList(fullLeaderboard.split("\n")));
		int pos = 0;
		String leaders = "";
		for(String line:leaderLines)
		{
			if(count == 0)
			{
				break;
			}
			if(pos >= topN)
			{
				count--;
				//grab user ID from line
				//grab user name, surname, and score from file
				String userID = line.split(":")[0].replaceAll("'", "");
				String score = line.split(":")[1].replaceAll("'", "");
				String userDataFile = "data/"+userID+"/data.json";
				String userData;
				if((userData = readFromS3(bucket, userDataFile, "leaderboard")).equals(null))
				{
					return null;
				}
				//Grab name and sName for leaders String
				JSONObject data = new JSONObject(userData);
				String name = data.get("Name").toString();
				String sName = data.get("Surname").toString();
				leaders = leaders + "{'Name':'" + name + "','Surname':'" + sName + "','Score':'" + score + "'}";
				leaders = leaders + ";";
			}
			pos++;
		}
		leaders = leaders.substring(0, leaders.length() - 1);
		return leaders;
	}
	
	private static String getUsers(String emailHash)
	{
		//returns a list of all users in format of {fName,sName,email}\n{fName,sName,email}\n etc
		if(emailHash.equals("*"))
		{
			String prefix = "data/";
			ArrayList<String> userFileList = getList(bucket, prefix);
			userFileList.remove(0);
			
			String userCSVList = "";
			for(String user : userFileList)
			{
				//Grab users first name, last name, email, create JSON object, convert to string, append to end of return
				//Grab user JSON data
				if(user.split("/")[2].equals("data.json"))
				{
					String data = "";
					if((data = readFromS3(bucket, user, "getUsers")).equals(null))
					{
						return null;
					}
					JSONObject dataJSON = new JSONObject(data);
					
					//Generate new JSON Object
					JSONObject userDetails = new JSONObject();
					
					//Grab first name, surname, email from data
					userDetails.put("Name",dataJSON.get("Name"));
					userDetails.put("Surname",dataJSON.get("Surname"));
					userDetails.put("Email",dataJSON.get("Email"));
					
					//Convert to string and append to end of userList
					userCSVList += userDetails.toString() + "\n";
				}
			}
			return userCSVList;
		}
		else //return full data + transaction history string for a single user
		{		
			/**Convert users emailHash to unique ID**/
			String userID;
			if((userID = userHashToID(emailHash, "getUsers")).equals(null))
			{
				return null;
			}
			
			String userData = "data/"+userID+"/data.json";
			String transHistory = "data/"+userID+"/purchaseHistory.json";
			if(!s3Client.doesObjectExist(bucket, userData))
			{
				return null;
			}
			
			//Get User Data
			String data;
			if((data = readFromS3(bucket, userData, "getUsers")).equals(null))
			{
				return null;
			}
			
			//Get User Transaction History
			String transData;
			if((transData = readFromS3(bucket, transHistory, "getUsers")).equals(null))
			{
				return null;
			}
			data += transData; //Append each line of transaction history into data String
			
			return data;
		}
	}
	
	private String stockHistory(String asxCode, int startDate, int endDate)
	{
		String prefix = asxCode+"/";
		String dataReturn = "";
		String ASXJSON = "asx-json-host";
		ArrayList<String> ASXfiles = getList(ASXJSON, prefix);
		for(String asxFile:ASXfiles)
		{
			int fileDate = Integer.parseInt(asxFile.split("[/.]")[1]);	//key should read "asx/20170214.json", fileDate should result in 20170214
			if(fileDate >= startDate && fileDate <= endDate)
			{
				//Open file, add contents to return data
				String fullStockData;
				if((fullStockData = readFromS3(ASXJSON, asxFile, "stockHistory")).equals(null))
				{
					return null;
				}
				ArrayList<String> stockLines = new ArrayList<String>(Arrays.asList(fullStockData.split("\n")));
				//Get last line of the stock's data
				String lastData = stockLines.get(stockLines.size()-1);
				String date = asxFile.split("[/.]")[1];
				JSONObject tmp = new JSONObject(lastData);
				JSONObject jData = new JSONObject();
				jData.put("ASX Code",  tmp.get("ASX Code"));
				jData.put("Date", date);
				jData.put("Ask Price", tmp.get("Ask Price"));
				dataReturn += jData.toString() + "\n";
			}
		}
		return dataReturn;
	}
	
	private boolean sendMessage(String senderEmailHash, String recipientEmailHash, String subject, String contents)
	{
		/**Convert senders emailHash to unique ID**/
		String senderID;
		if((senderID = userHashToID(senderEmailHash, "sendMessage")).equals(null))
		{
			return false;
		}
		String senderData;
		//Open senders data file and grab email field
		String senderDataFile = "data/"+senderID+"/data.json";
		if((senderData = readFromS3(bucket, senderDataFile, "sendMessage")).equals(null))
		{
			return false;
		}
		JSONObject senderJSON = new JSONObject(senderData);
		String senderEmail = senderJSON.getString("Email");
		
		/**Create JSON object to be posted in users mailbox**/
		JSONObject newMail = new JSONObject();
		newMail.put("Sender", senderEmail);
		newMail.put("Type", "message");
		newMail.put("Subject", subject);
		newMail.put("Contents", contents);
		newMail.put("Date", threadedConnection.date);
		newMail.put("Time", threadedConnection.time);
		newMail.put("Unread", "true");
		newMail.put("Deleted", "false");
		String mailEntry = newMail.toString();
		
		/**Convert recipients emailHash to unique ID**/
		String recipientID;
		if((recipientID = userHashToID(recipientEmailHash, "sendMessage")).equals(null))
		{
			return false;
		}
		/***************************************************************************************/
			
		/**Post message to recipients mailbox folder**/
		//Generate message number
		String prefix = "data/"+recipientID+"/mailbox/";
		ArrayList<String> mailList = getList(bucket, prefix);
		int mailID = 999; //minimum number for mailID is 1000
		int comp;
		for(String mail:mailList)
		{
			comp = Integer.parseInt(mail.split("[/.]")[3]);
			if(comp > mailID)
			{
				mailID = comp;
			}
		}
		//Generate new unique user ID
		mailID++;
		String mailPath = "data/"+recipientID+"/mailbox/"+mailID+".json";
		/***************************************************************************************/
			
		//Post mail to User
		if(!saveToS3(bucket, mailPath, mailEntry, "sendMessage"))
		{
			return false;
		}
		/***************************************************************************************/
		
		return true;
	}
	
	private String getMessageList(String emailHash, String type)
	{
		/**Convert users emailHash to unique ID**/
		String userID;
		if((userID = userHashToID(emailHash, "getMessageList")).equals(null))
		{
			return null;
		}
		
		//returns a list of message ID's
		String prefix = "data/"+userID+"/mailbox/";
		ArrayList<String> mailList = getList(bucket, prefix);
		String result = "";
		for(String mail:mailList)
		{
			String mailData;
			//Download mail object and check if it is actually a message
			if((mailData = readFromS3(bucket, mail, "getMessageList")).equals(null))
			{
				return null;
			}
			JSONObject mailJSON = new JSONObject(mailData);
			if(mailJSON.getString("Type").equals(type))
			{
				if(type.equals("message") && mailJSON.getString("Deleted").equals("false"))
				{
					result += Integer.parseInt(mail.split("[/.]")[3]) + ",";
				}
				else if(type.equals("funds"))
				{
					result += Integer.parseInt(mail.split("[/.]")[3]) + ",";
				}
			}
		}
		if(result.length() > 0)
		{
			result = result.substring(0, result.length()-1);
			return result;
		}
		else
		{
			return "0";
		}
	}
	
	private String getDeletedMessageList(String emailHash, String type)
	{
		/**Convert users emailHash to unique ID**/
		String userID;
		if((userID = userHashToID(emailHash, "getMessageList")).equals(null))
		{
			return null;
		}
		
		//returns a list of message ID's
		String prefix = "data/"+userID+"/mailbox/";
		ArrayList<String> mailList = getList(bucket, prefix);
		String result = "";
		for(String mail:mailList)
		{
			String mailData;
			//Download mail object and check if it is actually a message
			if((mailData = readFromS3(bucket, mail, "getMessageList")).equals(null))
			{
				return null;
			}
			JSONObject mailJSON = new JSONObject(mailData);
			if(mailJSON.getString("Type").equals(type) && mailJSON.getString("Deleted").equals("true"))
			{
				result += Integer.parseInt(mail.split("[/.]")[3]) + ",";
			}
		}
		if(result.length() > 0)
		{
			result = result.substring(0, result.length()-1);
			return result;
		}
		else
		{
			return "0";
		}
	}
	
	private String getMessage(String emailHash, int mailId)
	{
		/**Convert users emailHash to unique ID**/
		String userID;
		if((userID = userHashToID(emailHash, "getMessage")).equals(null))
		{
			return null;
		}
		
		/**Mark mail item as read and reupload**/
		String mailPath = "data/" + userID + "/mailbox/" + mailId + ".json";
		if(!s3Client.doesObjectExist(bucket, mailPath))
		{
			System.out.println("Mail item does not exist!");
			fileData += "getMessage: Mail item does not exist!\n";
			return null;
		}
		String mailData;
		if((mailData = readFromS3(bucket, mailPath, "getMessage")).equals(null))
		{
			return null;
		}
		/**Return Mail Item**/
		return mailData;	
	}
	
	private boolean deleteMessage(String emailHash, int mailId)
	{
		/**Convert users emailHash to unique ID**/
		String userID;
		if((userID = userHashToID(emailHash, "deleteMessage")).equals(null))
		{
			return false;
		}
		
		/**Delete Mail Item**/
		String mailPath = "data/" + userID + "/mailbox/" + mailId + ".json";
		if(!s3Client.doesObjectExist(bucket, mailPath))
		{
			
			System.out.println("Mail item does not exist!");
			fileData += "deleteMessage: Mail item does not exist!\n";
			return false;
		}
		//Grab message from storage
		String mailData;
		if((mailData = readFromS3(bucket, mailPath, "deleteMessage")).equals(null))
		{
			return false;
		}
		JSONObject mailJSON = new JSONObject(mailData);
		//If message has been moved to trash, remove message, else, move to trash
		if(mailJSON.getString("Type").equals("message"))
		{
			if(mailJSON.getString("Deleted").equals("true"))
			{
				s3Client.deleteObject(new DeleteObjectRequest(bucket, mailPath));
			}
			else
			{
				//Mark message as deleted and read and reupload
				mailJSON.put("Deleted", "true");
				mailJSON.put("Unread", "false");
				mailData = mailJSON.toString();
				if(!saveToS3(bucket, mailPath, mailData, "deleteMessage"))
				{
					return false;
				}
			}
		}
		else
		{
			s3Client.deleteObject(new DeleteObjectRequest(bucket, mailPath));
		}
		return true;
	}
	
	private String getUnreadMail(String emailHash)
	{
		/**Convert users emailHash to unique ID**/
		String userID;
		if((userID = userHashToID(emailHash, "getUnreadMail")).equals(null))
		{
			return null;
		}
		
		/**Get contents of unreadMail.csv for user**/
		//returns a list of message ID's
		String prefix = "data/"+userID+"/mailbox/";
		ArrayList<String> mailList = getList(bucket, prefix);
		String result = "";
		for(String mail:mailList)
		{
			String mailData;
			if((mailData = readFromS3(bucket, mail, "getUnreadMail")).equals(null))
			{
				return null;
			}
			JSONObject mailJSON = new JSONObject(mailData);
			if(mailJSON.getString("Type").equals("message") && mailJSON.getString("Unread").equals("true"))
			{
				int id = Integer.parseInt(mail.split("[/.]")[3]);
				result += id+",";
			}
		}
		if(!result.equals(""))
		{
			result = result.substring(0, result.length()-1);
			return result;
		}
		else
		{
			return "0";
		}
	}
	
	private boolean markMailUnread(String emailHash, int mailId)
	{
		/**Convert users emailHash to unique ID**/
		String userID;
		if((userID = userHashToID(emailHash, "markMailUnread")).equals(null))
		{
			return false;
		}
		
		/**Mark mail item as unread and reupload**/
		String mailPath = "data/" + userID + "/mailbox/" + mailId + ".json";
		if(!s3Client.doesObjectExist(bucket, mailPath))
		{
			System.out.println("Mail item does not exist!");
			fileData += "markMailUnread: Mail item does not exist!\n";
			return false;
		}
		String mailData;
		if((mailData = readFromS3(bucket, mailPath, "markMailUnread")).equals(null))
		{
			return false;
		}
		JSONObject mailJSON = new JSONObject(mailData);
		mailJSON.put("Unread", "true");
		mailData = mailJSON.toString();
		if(!saveToS3(bucket, mailPath, mailData, "markMailUnread"))
		{
			return false;
		}
		return true;
	}
	
	private boolean markMailRead(String emailHash, int mailId)
	{
		/**Convert users emailHash to unique ID**/
		String userID;
		if((userID = userHashToID(emailHash, "markMailUnread")).equals(null))
		{
			return false;
		}
		
		/**Mark mail item as unread and reupload**/
		String mailPath = "data/" + userID + "/mailbox/" + mailId + ".json";
		if(!s3Client.doesObjectExist(bucket, mailPath))
		{
			System.out.println("Mail item does not exist!");
			fileData += "markMailRead: Mail item does not exist!\n";
			return false;
		}
		String mailData;
		if((mailData = readFromS3(bucket, mailPath, "markMailRead")).equals(null))
		{
			return false;
		}
		JSONObject mailJSON = new JSONObject(mailData);
		mailJSON.put("Unread", "false");
		mailData = mailJSON.toString();
		if(!saveToS3(bucket, mailPath, mailData, "markMailRead"))
		{
			return false;
		}
		return true;
	}
	
	private boolean deleteAccount(String emailHash)
	{
		/**Convert users emailHash to unique ID**/
		String userID;
		if((userID = userHashToID(emailHash, "deleteAccount")).equals(null))
		{
			return false;
		}
		
		/**Remove user from leaderboard**/
		String leaderboardFile = "leaderboard.csv";
		String fullLeaderboard;
		//retrieve leaderboard file
		if((fullLeaderboard = readFromS3(bucket, leaderboardFile, "deleteAccount")).equals(null))
		{
			return false;
		}
		String newLeaderboard = "";
		String leaderboardParts[] = fullLeaderboard.split("[:\n]");
		for(int i=0; i<leaderboardParts.length; i+=2)
		{
			if(!leaderboardParts[i].equals(userID))
			{
				//Rebuild leaberboard file without the suer ID being deleted
				newLeaderboard += leaderboardParts[i] + ":" + leaderboardParts[i+1] + "\n";
			}
		}
		//Reupload updated leaderboard file
		if(!saveToS3(bucket, leaderboardFile, newLeaderboard, "deleteAccount"))
		{
			return false;
		}
		
		/**Delete user data files**/
		//Generate a list of the users files
		String prefix = "data/"+userID+"/";
		ArrayList<String> userFiles = getList(bucket, prefix);
		for(String file:userFiles)
		{
			s3Client.deleteObject(new DeleteObjectRequest(bucket, file));
		}
		
		/**Delete users credentials file**/
		String creds = "creds/"+emailHash+".rec";
		s3Client.deleteObject(new DeleteObjectRequest(bucket, creds));
		
		return true;
	}
	
	protected boolean sendFunds(String senderEmailHash, String recipientEmailHash, float amount)
	{
		/**Convert senders emailHash to unique ID**/
		String senderID;
		if((senderID = userHashToID(senderEmailHash, "sendFunds")).equals(null))
		{
			return false;
		}
		String senderData;
		
		//Open senders data file and grab email field
		String senderDataFile = "data/"+senderID+"/data.json";
		if((senderData = readFromS3(bucket, senderDataFile, "sendFunds")).equals(null))
		{
			return false;
		}
		JSONObject senderJSON = new JSONObject(senderData);
		String senderEmail = senderJSON.getString("Email");
		
		//Remove amount from senders balance
		senderJSON.put("Balance", Double.toString(senderJSON.getDouble("Balance") - amount));
		if(!saveToS3(bucket, senderDataFile, senderJSON.toString(), "sendFunds"))
		{
			return false;
		}
		
		
		/**Create JSON object to be posted in users mailbox**/
		JSONObject newFunds = new JSONObject();
		newFunds.put("Sender", senderEmail);
		newFunds.put("Type", "funds");
		newFunds.put("Amount", Double.toString(amount));
		newFunds.put("Unread", "true");
		newFunds.put("Date", threadedConnection.date);
		newFunds.put("Time", threadedConnection.time);
		String fundsEntry = newFunds.toString();
		
		/**Convert recipients emailHash to unique ID**/
		String recipientID;
		if((recipientID = userHashToID(recipientEmailHash, "sendFunds")).equals(null))
		{
			return false;
		}
		/***************************************************************************************/
		
		/**Post message to recipients mailbox folder**/
		//Generate message number
		String prefix = "data/"+recipientID+"/mailbox/";
		ArrayList<String> mailList = getList(bucket, prefix);
		int mailID = 999; //minimum number for mailID is 1000
		int comp;
		for(String mail:mailList)
		{
			comp = Integer.parseInt(mail.split("[/.]")[3]);
			if(comp > mailID)
			{
				mailID = comp;
			}
		}
		//Generate new unique user ID
		mailID++;
		String mailPath = "data/"+recipientID+"/mailbox/"+mailID+".json";
		/***************************************************************************************/
			
		//Post funds request to User
		if(!saveToS3(bucket, mailPath, fundsEntry, "sendFunds"))
		{
			return false;
		}
		/***************************************************************************************/
		
		return true;
	}
	
	protected boolean acceptFunds(String user, String fundID, float amount)
	{
		/**Convert senders emailHash to unique ID**/
		String recipientID;
		if((recipientID = userHashToID(user, "acceptFunds")).equals(null))
		{
			return false;
		}
		String recipientData;
		//Open senders data file and grab email field
		String recipientDataFile = "data/"+recipientID+"/data.json";
		if((recipientData = readFromS3(bucket, recipientDataFile, "acceptFunds")).equals(null))
		{
			return false;
		}
		JSONObject recipientJSON = new JSONObject(recipientData);
		String recipientEmail = recipientJSON.getString("Email");
		
		//Get funds message data
		String fundsPath = "data/"+recipientID+"/mailbox/"+fundID+".json";
		String fundsData = "";
		if((fundsData = readFromS3(bucket, fundsPath, "acceptFunds")).equals(null))
		{
			return false;
		}
		JSONObject fundsJSON = new JSONObject(fundsData);
		if(fundsJSON.getDouble("Amount") < amount)
		{
			return false;
		}
		
		//Delete the funds message
		s3Client.deleteObject(new DeleteObjectRequest(bucket, fundsPath));
		
		//Add accepted amount to recipients account
		Double recipientBalance = recipientJSON.getDouble("Balance");
		recipientJSON.put("Balance", Double.toString(recipientBalance + amount));
		recipientData = recipientJSON.toString();
		if(!saveToS3(bucket, recipientDataFile, recipientData, "acceptFunds"))
		{
			return false;
		}
		
		//Notifies sender of funds of the amount that was accepted.
		double refund = fundsJSON.getDouble("Amount") - amount;
		String adminHash = Integer.toString("admin@tradingwheels.com.au".hashCode());
		String refundUser = fundsJSON.getString("Sender");
		String refundHash = Integer.toString(refundUser.hashCode());
		String refundSubject = "Funds transfer " + fundID;
		String refundMessage;
		if(amount == 0)
		{
			refundMessage = "The user at " + recipientEmail + " has rejected your funds transfer.\n$" + refund + " has been added back into your account.";
		}
		else
		{
			refundMessage = "The user at " + recipientEmail + " has accpeted $" + amount + " of your funds transfer.\n$" + refund + " has been added back into your account.";
		}
		if(!sendMessage(adminHash, refundHash, refundSubject, refundMessage))
		{
			return false;
		}
		String senderID;
		if((senderID = userHashToID(refundHash, "acceptFunds")).equals(null))
		{
			return false;
		}
		//Add refunded amount to senders account
		String senderData;
		String senderDataFile = "data/"+senderID+"/data.json";
		if((senderData = readFromS3(bucket, senderDataFile, "acceptFunds")).equals(null))
		{
			return false;
		}
		JSONObject senderJSON = new JSONObject(senderData);
		Double senderBalance = senderJSON.getDouble("Balance");
		senderJSON.put("Balance", Double.toString(senderBalance + refund));
		senderData = senderJSON.toString();
		if(!saveToS3(bucket, senderDataFile, senderData, "acceptFunds"))
		{
			return false;
		}
		return true;
	}
}
