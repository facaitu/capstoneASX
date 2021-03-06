package com.amazonaws.samples;

import java.util.*;

import org.json.JSONObject;

//temp control file while the project is console operated
public class Menus
{	
	private static Scanner consoleRead = new Scanner(System.in);
	private static String input; 
	private static int inputInt;
	private static int qty;
	private static float amount;
	
	protected static void menuLogin()
	{		
		System.out.println("\nPlease select on of the following options: \n");
		System.out.println("	1. Login with existing account");
		System.out.println("	2. Register new player");
		System.out.println("	3. Get temp asx data + print");
		System.out.println("	0. Exit");
		input = consoleRead.next();
		inputInt = Integer.parseInt(input);
		switch (inputInt)
		{
			case 1:
				menuLoginDialogue();
				return;
			case 2:
				menuRegisterDialogue();
				return;
			case 3:
				//Utilities.loadTempStockList();
				AsxPull.getAsxJson("ABP", "20170329");
				AsxGame.stockArray.get(0).printStock();
				AsxGame.stockArray.remove(0);
				return;
			case 0: 		//exit
				System.exit(0);
				break;
		}
	}
	
	protected static void menuLoginDialogue()
	{
		String uEmail, password;
		System.out.println("Please enter username/email address: ");
		uEmail = consoleRead.next();
		System.out.println("Please enter password: ");
		password = consoleRead.next();
		Game.login(uEmail, password);
	}
	
	protected static void menuRegisterDialogue()
	{
		String name, surname, email, emailCheck, password, pwCheck;
		boolean emailCorrect = false;
		boolean passCorrect = false;
		boolean allCorrect = false;
		do 
		{
			System.out.println("Please enter first name: ");
			name = consoleRead.next();
			System.out.println("Please enter surname: ");
			surname = consoleRead.next();
			do 
			{
				System.out.println("Please enter email address: ");
				email = consoleRead.next();
				System.out.println("Please re-enter email address: ");
				emailCheck = consoleRead.next();
				if (email.equals(emailCheck))
				{
					emailCorrect = true;
				} 
				else 
				{
					System.out.println("Email adresses are different, please try again!");
				}			
			} while (emailCorrect == false);
			do 
			{
				System.out.println("Please enter pasasword: ");
				password = consoleRead.next();
				System.out.println("Please re-enter password: ");
				pwCheck = consoleRead.next();
				if (password.equals(pwCheck))
				{
					passCorrect = true;
				}
				else
				{
					System.out.println("passwords are different, please try again!");
				}			
			} while (passCorrect == false);
			System.out.println("Your entered details are: ");
			System.out.println("Name: " + name + " " + surname);
			System.out.println("Email: " + email);
			System.out.println("Is this correct? Y/N");
			input  = consoleRead.next();
			System.out.println(input);
			if (input.equals("y") || input.equals("Y"))
			{
				System.out.println("Attempting to register player");
				Game.registerPlayer(name, surname, email, password);
				allCorrect = true;
			}
		} while (allCorrect == false);
		
		
	}
	
	protected static void buyStockDialogue()
	{				//all dialogue for buying stocks
		if (AsxGame.stockArray.size() != 0)
		{
			System.out.println("All available stocks: ");
			for (int i = 0; i < AsxGame.stockArray.size(); i++)
			{
				AsxGame.stockArray.get(i).printStock();
			}
			System.out.println("Please select a stock to buy (give code, case sensitive)");
			input = consoleRead.next();
			for (int index = 0; index < AsxGame.stockArray.size(); index++)
			{
				if (input.equals(AsxGame.stockArray.get(index).code))
				{
					System.out.println("Purchasing stock :" + AsxGame.stockArray.get(index).name);
					System.out.println("Enter number you wish to purchase");
					String input2 = consoleRead.next();
					inputInt = Integer.parseInt(input2);
					boolean success = Game.buyStocks(input, inputInt);
					if (success == true)
					{
						System.out.println("Shares purchased");
					}
					else if (success == false)
					{
						System.out.println("Shares NOT purchased");
					}
				}
			}
		}
	}
	
	protected static void sellStockDialogue()
	{					//all dialogue for selling stocks
		if (AsxGame.stockArray.size() != 0)
		{
			System.out.println("Players stocks: ");
			AsxGame.activePlayer.printShares();
			System.out.println("Please select a stock to sell (give code, case sensitive)");
			input = consoleRead.next();
			for (int index = 0; index < AsxGame.activePlayer.shares.size(); index++)
			{
				String[] shareSplit = AsxGame.activePlayer.shares.get(index).split(":");
				if (input.equals(shareSplit[0]))
				{
					System.out.println("Selling stock :" + shareSplit[0]);
					System.out.println("Enter number you wish to sell");
					input = consoleRead.next();
					inputInt = Integer.parseInt(input);
					if (Game.sellStocks(shareSplit[0], inputInt))
					{
						System.out.println("Shares Sold");
					}
				}
			}
		}
	}
	
	protected static void mainMenu()
	{
		while (true)
		{		
			System.out.println("\nPlease select on of the following options: \n");
			System.out.println("	1. Print Active Player Details");
			System.out.println("	2. List all availble stocks");
			System.out.println("	3. Buy stocks");
			System.out.println("	4. Sell stocks");
			System.out.println("	5. Save player to server");
			System.out.println("	6. Get Leaderboard");
			System.out.println("	7. Send Message");
			System.out.println("	8. View a message");
			System.out.println("	9. Delete a message");
			System.out.println("	10. View list of unread messages");
			System.out.println("	11. View list of deleted messages");
			System.out.println("	12. Mark message as unread");
			System.out.println("	13. Send funds to user");
			System.out.println("	14. View and Accept an incoming funds transfer(s)");
			System.out.println("	15. View history of stock");
			System.out.println("	16. !!!Delete your account!!!");
			System.out.println("	20. Logout (NOTE: THIS DOES NOT SAVE YOUR PLAYER CURRENTLY!");
			System.out.println("	0. Exit");
			
			input = consoleRead.next();
			inputInt = Integer.parseInt(input);
			switch (inputInt) 
			{
				case 1:		//Print Active Player Details
					AsxGame.activePlayer.printPlayer();
					break;
				case 2: 		//List all available stocks
					System.out.println(AsxGame.stockArray.size());
					for (int i = 0; i < AsxGame.stockArray.size(); i++)
					{
						AsxGame.stockArray.get(i).printStock();
					}
					break;
				case 3:		//Buy stocks
					buyStockDialogue();
					break;
				case 4:		//Sell Stocks
					sellStockDialogue();
					break;
				case 5:		//Save player to server
					//System.out.println(AsxGame.activePlayer.generateSaveString());
					Game.saveActivePlayer(null);
					break;
				case 6:
					Game.getValueLeaderboard();
					for (int i = 0; i < AsxGame.leaderboard.size(); i++)
					{
						System.out.println(AsxGame.leaderboard.get(i));
					}
					break;
				case 7: //Send Message
					System.out.println("Enter recipient, then subject line, then message");
					String recipient = consoleRead.next();
					String subject = consoleRead.next();
					String message = consoleRead.next();
					Game.sendMessage(null, recipient, subject, message);
					break;
				case 8: //View Message
					System.out.println("Retrieving list of messages");
					ArrayList<Integer> messageList = AsxGame.activePlayer.messages;
					if(!messageList.get(0).equals("204") && messageList != null)
					{
						System.out.println("Messages available:");
						for(int id:messageList)
						{
							System.out.println(id);
						}
						System.out.println("Please enter a message ID to view...");
						int mID = Integer.parseInt(consoleRead.next());
						String mailItem = Game.getMessage(mID);
						if(mailItem != null)
						{
							JSONObject mailJSON = new JSONObject(mailItem);
							System.out.println("Received: " + mailJSON.getString("Date") + "-" + mailJSON.getString("Time"));
							System.out.println("Message from: " + mailJSON.getString("Sender"));
							System.out.println("Message type: " + mailJSON.getString("Type"));
							System.out.println("Message subject: " + mailJSON.getString("Subject"));
							System.out.println("Message contents: " + mailJSON.getString("Contents"));
						}
					}
					break;
				case 9: //Delete a message
					System.out.println("Retrieving list of messages");
					messageList = AsxGame.activePlayer.messages;
					if(!messageList.get(0).equals("204") && messageList != null)
					{
						System.out.println("Messages available:");
						for(int id:messageList)
						{
							System.out.println(id);
						}
						System.out.println("Please enter a message ID to delete...");
						int mID = Integer.parseInt(consoleRead.next());
						Game.deleteMessage(mID);
						int index = AsxGame.activePlayer.messages.indexOf(mID);
						AsxGame.activePlayer.messages.remove(index);
						System.out.println("Message deleted");
					}
					break;
				case 10: //Retrieve list of unread messages
					System.out.println("Retrieving list of unread messages");
					messageList = AsxGame.activePlayer.unreadMessages;
					if(messageList != null)
					{
						System.out.println("Unread Messages:");
						for(int id:messageList)
						{
							System.out.println(id);
						}
					}
					break;
				case 11: //Retrieve list of unread messages
					System.out.println("Retrieving list of deleted messages");
					messageList = AsxGame.activePlayer.deletedMessages;
					if(messageList != null)
					{
						System.out.println("Deleted Messages:");
						for(int id:messageList)
						{
							System.out.println(id);
						}
					}
					break;
				case 12: //Mark message as unread
					System.out.println("Retrieving list of messages");
					messageList = AsxGame.activePlayer.messages;
					if(!messageList.equals("204") && messageList != null)
					{
						System.out.println("Messages available:");
						for(int id:messageList)
						{
							System.out.println(id);
						}
						System.out.println("Please enter a message ID to mark as unread...");
						int mID = Integer.parseInt(consoleRead.next());
						Game.markUnread(mID);
						AsxGame.activePlayer.unreadMessages.add(mID);
						System.out.println("Message marked unread!");
					}
					break;
				case 13: //Send funds to user
					System.out.println("Enter username of recipient, and then amount you wish to send");
					recipient = consoleRead.next();
					float amount = Float.parseFloat(consoleRead.next());
					if(Game.sendFunds(recipient, amount))
					{
						System.out.println("Funds sent successfully");
					}
					else
					{
						System.out.println("500: INTERNAL SERVER ERROR!");
					}
					break;
				case 14: //Accept a funds transfer
					System.out.println("Retrieving list of fund transfers");
					ArrayList<Integer> fundsList = AsxGame.activePlayer.pendingFunds;
					if(!fundsList.get(0).equals("204\n") && fundsList != null)
					{
						System.out.println("Messages available:");
						for(int id:fundsList)
						{
							System.out.println(id);
						}
						System.out.println("Please enter a fund ID to view...");
						String fundID = consoleRead.next();
						int fID = Integer.parseInt(fundID);
						String mailItem = Game.getMessage(fID);
						if(mailItem != null)
						{
							JSONObject mailJSON = new JSONObject(mailItem);
							System.out.println("Received: " + mailJSON.getString("Date") + "-" + mailJSON.getString("Time"));
							System.out.println("Message from: " + mailJSON.getString("Sender"));
							System.out.println("Amount sent: " + mailJSON.getString("Amount"));
						}
						System.out.println("Enter the amount you wish to accept.");
						amount = Float.parseFloat(consoleRead.next());
						Game.acceptFunds(fundID, amount);
					}
					break;
				case 15: //Get history of stock
					System.out.println("Enter stock code");
					String code = consoleRead.next();
					System.out.println("Enter start date (yyyymmdd)");
					int startDate = Integer.parseInt(consoleRead.next());
					System.out.println("Enter end date (yyyyymmdd)");
					int endDate = Integer.parseInt(consoleRead.next());
					Game.getStockHistory(code, startDate, endDate);
					System.out.println(AsxGame.requestedStockHistory.toString());
					break;
				case 16: //Delete self
					Game.playerDeleteSelf();
					AsxGame.activePlayer = null;
					AsxGame.activePlayerLoaded = false;
					return;
				case 20: 		//logout
					Game.logout();
					return;					
				case 0: 		//exit
					System.exit(0);
					break;
				default: System.out.println("Invalid Choice!");
			}
		}
	}

	protected static void adminMenu()
	{
		while (true)
		{		
			System.out.println("\nPlease select on of the following options: \n");
			System.out.println("	1. View admin account details");
			System.out.println("	2. Load a player to modify");
			System.out.println("	3. View loaded player account details");
			System.out.println("	4. Add stocks to loaded player");
			System.out.println("	5. Remove stocks from loaded player");
			System.out.println("	6. Set loaded players balance");
			System.out.println("	7. Save + unload loaded player");
			System.out.println("	9. Logout (NOTE: THIS DOES NOT SAVE YOUR PLAYER CURRENTLY!");
			System.out.println("	10. Set brokers fee on purchases");
			System.out.println("	11. Set brokers fee on sales");
			System.out.println("	12. Message all users");
			System.out.println("	13. Delete a user");
			System.out.println("	14. Convert user email to ID");
			System.out.println("	0. Exit");
			
			input = consoleRead.next();
			inputInt = Integer.parseInt(input);
			switch (inputInt) 
			{
				case 1:		//Print Active Admin Details
					AsxGame.activeAdmin.printPlayer();
					break;
				case 2: 		//load player for admin to modify
					Admin.getUserList();
					ArrayList<String> playerList = Admin. returnPlayerList();
					for(String player:playerList)
					{
						System.out.println(player);
					}
					System.out.println("Enter a user email address");
					input = consoleRead.next();
					Admin.adminLoadPlayer(input);
					break;				
				case 3:		//View loaded player account details
					AsxGame.activePlayer.printPlayer();
					break;
				case 4:		//Add stocks to loaded player
					System.out.println("Enter a stock code, then qty to add to user");
					input = consoleRead.next();
					qty = Integer.parseInt(consoleRead.next());
					Admin.addStock(input, qty);
					break;
				case 5:		//Remove stocks from loaded player
					System.out.println("Enter a stock code, then qty to remove from user");
					input = consoleRead.next();
					qty = Integer.parseInt(consoleRead.next());
					Admin.removeStock(input, qty);
					break;
				case 6:		//Set loaded players balance
					System.out.println("Enter a number to change users balance to");
					amount = Float.parseFloat(consoleRead.next());
					Admin.changeBalance(amount);
					break;
				case 7:		//Save + unload loaded player
					System.out.println("Unloading player");
					Admin.adminUnloadPlayer();
					break;
				case 9: 		//logout
					Game.logout();
					return;
				case 10: 		//set buy fees
					System.out.println("Enter flat fee then percentage fee");
					float flat = Float.parseFloat(consoleRead.next());
					float per = Float.parseFloat(consoleRead.next());
					Admin.setBuyFee(flat, per);
					break;
				case 11: 		//set sell fees
					System.out.println("Enter flat fee then percentage fee");
					flat = Float.parseFloat(consoleRead.next());
					per = Float.parseFloat(consoleRead.next());
					Admin.setSellFee(flat, per);
					break;
				case 12: //Message all users
					System.out.println("Enter subject line then message to send to all users");
					String subject = consoleRead.next();
					String message = consoleRead.next();
					Admin.messageAllUsers(subject, message);
					break;
				case 13: //Delete a user
					System.out.println("Enter the user name of the user you want to delete");
					String user = consoleRead.next();
					System.out.println("Please re-enter the username to confirm");
					String userConfirm = consoleRead.next();
					if(user.equals(userConfirm))
					{
						if(!Admin.deleteUser(user))
						{
							System.out.println("Server Error!");
						}
						else
						{
							System.out.println(user + " deleted!");
						}
					}
					else
					{
						System.out.println("Usernames don't match!");
					}
					break;
				case 14: //Get user ID from email
					System.out.println("Enter the username you want to convert to ID number.");
					user =  consoleRead.next();
					String response = "";
					if((response = Admin.getID(user)) != null)
					{
						System.out.println(user + " -> " + response);
					}
					else
					{
						System.out.println("Server Error!");
					}
					break;
				case 0: 		//exit
					System.exit(0);
					break;
				default: System.out.println("Invalid Choice!");
			}
		}
	}
}	
