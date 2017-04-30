/*
 	Admin class
 	
 	All admin functionality should be placed here
 	needs work
  
 */

package com.amazonaws.samples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.json.JSONObject;

public class Admin {
	
	ArrayList<String> playerList = new ArrayList<String>();
	
	protected void adminUnloadPlayer(){
		
	}
	
	//gets a list of all users
	//I do not know if this works, I have not tested it - Shane
	protected boolean getUserList(){
		Socket connection = null;
		boolean successState = false;
		try{
			connection = new Socket(AsxGame.connectionName, AsxGame.portNumber);
			
			//Input Streams
			BufferedReader connectionRead = null;
			String response = null;
			
    		//Output Streams
			DeflaterOutputStream deflStream = null;
			
			try{
				// call
				deflStream = new DeflaterOutputStream(connection.getOutputStream(), true);
				System.out.println("Attempt getUserList...");
				String sendString = "getUser\n*";
				byte[] sendBytes = sendString.getBytes("UTF-8");
				deflStream.write(sendBytes);
				deflStream.finish();
				deflStream.flush();
				
				while (true){
					connectionRead = new BufferedReader(new InputStreamReader(new InflaterInputStream(connection.getInputStream())));
					response = connectionRead.readLine();
					if(response != null)
					{
						if(!response.equals("500"))
						{
							while (response != null){
								playerList.add(response);
								response = connectionRead.readLine();
							}
							successState = true;
						}
						else
						{
							System.out.println("500: INTERNAL SERVER ERROR!");
							successState = false;
							break;
						}
					}
				}
				
			} catch (IOException e) {
				
			}
			
		} catch (IOException e) {
			System.out.println("Exception while opening connection: " + e);
			successState = false;
		}
		try{
			connection.close();
		}catch (IOException e) {
			System.out.println("Exception while closing connection: " + e);
			successState = false;
		}
		
		return successState;
	}
	
	//Gets a users data String from the server - Cal
	//user is the desired users email address
	protected void adminLoadPlayer(String user)
	{
		String emailHash = Integer.toString(user.hashCode());
		Socket connection = null;
		try
		{
			connection = new Socket(AsxGame.connectionName, AsxGame.portNumber);
			
			//Input Streams
			BufferedReader connectionRead = null;
			String response = null;
			
    		//Output Streams
			DeflaterOutputStream deflStream = null;
			
			try
			{
				// call
				deflStream = new DeflaterOutputStream(connection.getOutputStream(), true);
				System.out.println("Attempt getUser...");
				String sendString = "getUser\n"+emailHash;
				byte[] sendBytes = sendString.getBytes("UTF-8");
				deflStream.write(sendBytes);
				deflStream.finish();
				deflStream.flush();
				while (true)
				{
					connectionRead = new BufferedReader(new InputStreamReader(new InflaterInputStream(connection.getInputStream())));
					response = connectionRead.readLine();
					System.out.println(response);
					if(response != null)
					{
						if(!response.equals("500"))
						{
							Game.loadPlayer(response);
							while((response = connectionRead.readLine()) != null)
							{
								JSONObject histLine = new JSONObject(response);
								AsxGame.activePlayer.transHistory.add(histLine);
							}
							connectionRead.close();
							break;
						}
						else
						{
							System.out.println("500: INTERNAL SERVER ERROR!");
							break;
						}
					}
				}
				
			} 
			catch (IOException e) 
			{
				System.out.println("Exception while communicating with server: " + e);
			}
		} 
		catch (IOException e) 
		{
			System.out.println("Exception while opening connection: " + e);
		}
		return;
	}
	
	protected boolean changeBalance(float newBalance){
		return false;
	}
	
	protected boolean addStock(String asxCode, int number){
		return false;
	}
	
	protected boolean removeStock(String asxCode, int number){
		return false;
	}
	
	
}
