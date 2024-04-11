import java.io.*;
import java.net.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

//主函数
public class Server {
	static int port = 2525;
	public static void main(String[] args) {
		System.out.println("服务器，启动！！！");
		try{
			//创建监听套接字
			ServerSocket serverSocket = new ServerSocket(port);
			//循环监听客户端连接
			while(true){
				//获取客户端套接字
				Socket clientSocket = serverSocket.accept();
				connectionMode connectionmode = new connectionMode();
				String account = connectionmode.connectionAccount(clientSocket);
				//验证密码，密码正确才会进入后续操作
				String connectionModeMessage = "";
				while(true){
					//读入密码报文，报文形式：PASS sp <password>
					BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					connectionModeMessage = reader.readLine();
					if(connectionModeMessage != null && connectionModeMessage.startsWith("PASS") && connectionmode.connectionPassword(account, connectionModeMessage, clientSocket)){
						break;
					}
				}
				while(true){
					//监听到BYE后断开连接
					if(connectionmode.connectionOption(account, clientSocket)){
						PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
						System.out.println("用户断开连接：BYE");
						out.println("BYE");
						break;
					}
				}
				//关闭连接
				clientSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

//数据库操作类，用于操作数据
class manipulateData{
	private String userName = "root";
	private String password = "150790";
	private String databaseName = "userInformation";
	private String URL = String.format("jdbc:mysql://localhost:3306/%s?user=%s&password=%s&useSSL=false&allowPublicKeyRetrieval=true",databaseName,userName,password);
	//连接到数据库
	Connection connectionToDataBase(String connectionModeCode){
		try{
			//调用Class.forName()加载驱动
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e){
			e.printStackTrace();
		}
		try{
			//连接到数据库databaseName
			Connection connection;
			connection = DriverManager.getConnection(URL,userName,password);
			System.out.println("数据库连接成功：" + connectionModeCode);
			return connection;
		} catch (SQLException e){
			System.out.println("数据库连接失败：" + connectionModeCode);
			e.printStackTrace();
		}
		return null;
	}
	void connectionClose(Connection connection,PreparedStatement preparedStatement, ResultSet resultSet){
		try {
			if(connection != null){
				connection.close();
			}
			if(preparedStatement != null){
				preparedStatement.close();
			}
			if(resultSet != null){
				resultSet.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	//记录日志
	void recordOption(String account, String stateCode, String errorReason, Connection connection){
		LocalDateTime time = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		String optionTime = time.format(formatter);
		PreparedStatement mysql = null;
		try {
			String query;
			if(errorReason.equals("0")) {
				query = "insert atm_record (record_time, record_account, state_code) values (?, ?, ?)";
				mysql = connection.prepareStatement(query);
				mysql.setString(1, optionTime);
				mysql.setString(2, account);
				mysql.setString(3, stateCode);
			} else {
				query = "insert atm_record (record_time, record_account, state_code, error_reason) values (?, ?, ?, ?)";
				mysql = connection.prepareStatement(query);
				mysql.setString(1, optionTime);
				mysql.setString(2, account);
				mysql.setString(3, stateCode);
				mysql.setString(4, errorReason);
			}
			mysql.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			connectionClose(connection, mysql, null);
		}
	}

	/*实现客户端相关功能*/
	//登录
	String login(String account, String password, Connection connection){
		PreparedStatement mysql = null;
		ResultSet result = null;
		try{
			//检索用户账户
			String query = "select* from atm_userinformation where user_account = ?";
			mysql = connection.prepareStatement(query);
			mysql.setString(1, account);
			//执行SQL
			result = mysql.executeQuery();
			//比对检索结果
			if(result.next()){
				//密码正确
				String passwordResult = result.getString("user_password");
				if(password.equals(passwordResult)){
					return "525 OK!";
				} else {
					//密码错误
					recordOption(account, "401 ERROR", "Login: Password Error", connection);
					return "401 ERROR!";
				}
			} else {
				//账户不存在
				recordOption(account, "401 ERROR", "Login: Account Does Not Exist", connection);
				return "401 ERROR!";
			}
		} catch (SQLException e){
			System.out.println("数据库检索失败");
			e.printStackTrace();
		} finally {
			connectionClose(null, mysql, result);
		}
		recordOption(account, "401 ERROR", "Login: DateBase Error", connection);
		return "401 ERROR!";
	}
	String subtractExpense(String account, int optionNumber, Connection connection){
		PreparedStatement mysql = null;
		ResultSet result = null;
		try{
			String query = "select* from atm_userinformation where user_account = ?";
			mysql = connection.prepareStatement(query);
			mysql.setString(1, account);
			//执行SQL
			result = mysql.executeQuery();
			if(result.next()){
				//执行支出操作
				int balance = result.getInt("balance");
				if(balance >= optionNumber){
					//余额足够支出，进行操作
					balance -= optionNumber;
					query = String.format("update atm_userinformation set balance = ? where user_account = %s", account);
					mysql = connection.prepareStatement(query);
					mysql.setInt(1, balance);
					//执行更新语句
					mysql.executeUpdate();
					recordOption(account, "525 OK", String.format("DrawMoney: Subtract Expense: %s, Changed Balance: %s", optionNumber, balance), connection);
					return "525 OK!";
				} else {
					//余额不足
					recordOption(account, "401 ERROR", String.format("DrawMoney: Balance Does Not Enough, Balance: %s", balance), connection);
					return "401 ERROR!";
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			connectionClose(null, mysql, result);
		}
		recordOption(account, "401 ERROR", "DrawMoney: DateBase Error", connection);
		return null;
	}
	//余额查询
	String checkBalance(String account, Connection connection){
		PreparedStatement mysql = null;
		ResultSet result = null;
		try {
			String query = "select * from atm_userinformation where user_account = ?";
			mysql = connection.prepareStatement(query);
			mysql.setString(1,account);
			result = mysql.executeQuery();
			result.next();
			return result.getString("balance");
		} catch (SQLException e)
		{
			System.out.println("数据库检索失败");
			e.printStackTrace();
		} finally {
			connectionClose(connection, mysql, result);
		}
		return null;
	}
}

//Server连接类，用于不同操作下服务端的响应
class connectionMode{
	String connectionAccount(Socket clientSocket){
		try{
			//获取登录请求报文，以空格为界限分割；报文形式：HELO <account>
			BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String connectionModeMessage = reader.readLine();
			//终端输出登录请求报文
			System.out.println("用户连接成功：" + connectionModeMessage);
			//创建输出流向服务端返回结果
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),true);
			//从报文中获取账户
			String[] ModeMessages = connectionModeMessage.split("\\s+");
			String account = ModeMessages[1];
			//向客户端返回响应报文：500 sp AUTH REQUIRE
			out.println("500 AUTH REQUIRED!");
			return account;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}
	boolean connectionPassword(String account, String connectionModeMessage, Socket clientSocket){
		boolean isOK = false;
		try {
			//终端输出密码报文
			System.out.println("用户连接成功：" + connectionModeMessage);
			//从报文中获取密码
			String[] ModeMessages = connectionModeMessage.split("\\s+");
			String password = ModeMessages[1];
			//进行验证
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),true);
			manipulateData manipulateData = new manipulateData();
			String result = manipulateData.login(account, password, manipulateData.connectionToDataBase(connectionModeMessage));
			//若验证成功，返回true
			if(result.equals("525 OK!")){
				isOK = true;
			}
			//向客户端发送结果报文
			out.println(result);
			return isOK;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	boolean connectionOption(String account, Socket clientSocket){
		boolean exit = false;
		try {
			//读入操作报文，报文形式：BALA || WDRA <amount> || BYE
			BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String connectionModeMessage = reader.readLine();
			//创建输出流对象用于向服务端返回报文
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),true);
			//若为BYE报文，则结束此次连接
			if(connectionModeMessage.equals("BYE")){
				exit = true;
				out.println("BYE");
				return exit;
			}
			//接下来的操作均需要连接到数据库，先创建数据操作对象
			manipulateData manipulateData = new manipulateData();
			//查询余额，结果报文形式：AMNT:<amnt>
			if(connectionModeMessage.equals("BALA")){
				String balance = manipulateData.checkBalance(account, manipulateData.connectionToDataBase(connectionModeMessage));
				out.println(String.format("AMNT:%s", balance));
				return exit;
			}
			/*支出*/
			//获取支出金额
			String[] ModeMessages = connectionModeMessage.split("\\s+");
			int optionNumber = Integer.parseInt(ModeMessages[1]);
			//进行支出操作
			out.println(manipulateData.subtractExpense(account, optionNumber, manipulateData.connectionToDataBase(connectionModeMessage)));
			return exit;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}