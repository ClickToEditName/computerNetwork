import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ATM_Client {
  private JFrame frame;
  private JPanel cardPanel;
  private CardLayout cardLayout;
  private Socket clientSocket;
  //private DataOutputStream outToServer;
  private BufferedReader inFromServer;
  //private JTextField cardTextField;
  //private JPasswordField passwordField;
  //private JTextField amountTextField;

  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeLater(() -> { 
      ATM_Client client = new ATM_Client();
      Socket clientSocket = client.connectToServer();
      client.createAndShowGUI(clientSocket);
    });
  }

  public void createAndShowGUI(Socket clientSocket) {


    frame = new JFrame("ATM");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(400, 300);
    frame.setLocationRelativeTo(null);

    cardLayout = new CardLayout();
    cardPanel = new JPanel(cardLayout);

    // 插卡界面
    JPanel insertCardPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(5, 5, 5, 5);

    JLabel cardLabel = new JLabel("卡号");
    gbc.gridx = 0;
    gbc.gridy = 0;
    insertCardPanel.add(cardLabel, gbc);

    JTextField cardTextField = new JTextField(15);
    gbc.gridx = 1;
    gbc.gridy = 0;
    insertCardPanel.add(cardTextField, gbc);

    JButton insertCardButton = new JButton("插卡");
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 2;
    insertCardPanel.add(insertCardButton, gbc);

    // 密码界面
    JPanel passwordPanel = new JPanel(new GridBagLayout());
    JLabel passwordLabel = new JLabel("密码");
    gbc.gridx = 0;
    gbc.gridy = 0;
    passwordPanel.add(passwordLabel, gbc);

    JPasswordField passwordField = new JPasswordField(15);
    gbc.gridx = 1;
    gbc.gridy = 0;
    passwordPanel.add(passwordField, gbc);

    JButton confirmButton = new JButton("确认");
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    passwordPanel.add(confirmButton, gbc);

    // 菜单界面
    JPanel menuPanel = new JPanel(new GridBagLayout());
    JButton balanceButton = new JButton("查询余额");
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    menuPanel.add(balanceButton, gbc);

    JButton withdrawButton = new JButton("取款");
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 2;
    menuPanel.add(withdrawButton, gbc);

    JButton returnButton=new JButton("退卡");
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    menuPanel.add(returnButton, gbc);

    // 余额界面
    JPanel balancePanel = new JPanel(new GridBagLayout());
    JTextArea balanceTextArea = new JTextArea(5, 20);
    balanceTextArea.setEditable(false);
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    balancePanel.add(balanceTextArea, gbc);

    JButton returnToMenuButtonFromBalance = new JButton("返回菜单");
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 2;
    balancePanel.add(returnToMenuButtonFromBalance, gbc);

    // 取款界面
    JPanel withdrawPanel = new JPanel(new GridBagLayout());
    JTextField amountTextField = new JTextField(15);
    gbc.gridx = 0;
    gbc.gridy = 0;
    withdrawPanel.add(amountTextField, gbc);

    JButton withdrawButtonFromWithdraw = new JButton("取款");
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    withdrawPanel.add(withdrawButtonFromWithdraw, gbc);

    JButton returnToMenuButtonFromWithdraw = new JButton("返回菜单");
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    withdrawPanel.add(returnToMenuButtonFromWithdraw, gbc);

    // 将所有界面添加到卡片布局面板
    cardPanel.add(insertCardPanel, "insertCard");
    cardPanel.add(passwordPanel, "password");
    cardPanel.add(menuPanel, "menu");
    cardPanel.add(balancePanel, "balance");
    cardPanel.add(withdrawPanel, "withdraw");

    // 默认显示插卡界面
    cardLayout.show(cardPanel, "insertCard");
    frame.getContentPane().add(cardPanel);
    frame.setVisible(true);



    //为插卡按钮添加事件监听器
    insertCardButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(cardTextField.getText().isEmpty()){
          JOptionPane.showMessageDialog(null, "请插卡(输入正确的卡号)", "警告", JOptionPane.WARNING_MESSAGE);
        }else{
          String cardNumber = cardTextField.getText();
          String heloMessage = "HELO " + cardNumber;
          sendHELO(heloMessage,clientSocket);
        }
      }
    });

    //为确认按钮添加事件监听器
    confirmButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String password = new String(passwordField.getPassword());
        String passMessage = "PASS " + password;
        if(password.isEmpty()){
          JOptionPane.showMessageDialog(null, "请输入密码！", "警告", JOptionPane.WARNING_MESSAGE);
        }else{
          sendPIN(passMessage,clientSocket);

        }
          
      }
    });

    //为查询余额按钮添加事件监听器
    balanceButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          String balaMessage = "BALA";
          String balance_amount=sendBALA(balaMessage, clientSocket);
          balanceTextArea.setText(balance_amount);
        }
      });


    //为取款按钮添加事件监听器
    withdrawButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.show(cardPanel, "withdraw");
      }
    });

    //为确认取款按钮添加事件监听器
    withdrawButtonFromWithdraw.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(amountTextField.getText().isEmpty()){
          JOptionPane.showMessageDialog(null, "请输入正确的金额", "警告", JOptionPane.WARNING_MESSAGE);
        }else{
          String amount_withdraw = amountTextField.getText();
          String wdraMessage = "WDRA " + amount_withdraw;
          sendWDRA(wdraMessage, clientSocket);
          //endHELO(heloMessage,clientSocket);
        }
      }
    });

    //为返回菜单按钮添加事件监听器
    returnToMenuButtonFromBalance.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.show(cardPanel, "menu");
      }
    });

    returnToMenuButtonFromWithdraw.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.show(cardPanel, "menu");
      }
    });

    //为退卡按钮添加事件监听器
    returnButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String byeMessage = "BYE";
        sendBYE(byeMessage, clientSocket,frame);
      }
    });

    

  }

  //发送HELO报文
  private void sendHELO(String heloMessage,Socket serverSocket) {
  
    try {

      // 创建输入流和输出流
      BufferedReader reader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
      PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);

      // 发送HELO报文
      out.println(heloMessage+ '\n');

      // 接收服务器的响应报文
      String response = reader.readLine();
      System.out.println("服务器响应：" + response);

      // 处理服务器的响应报文
      if (response.equals("500 AUTH REQUIRED!")) {
          // 切换到密码界面
          //switchToPasswordScreen();
          cardLayout.show(cardPanel, "password");
      } else {
          // 弹窗警告检查卡号是否正确
          JOptionPane.showMessageDialog(frame, "请检查卡号是否正确！", "警告", JOptionPane.WARNING_MESSAGE);
      }

      // 关闭连接
      //serverSocket.close();
    } catch (IOException ex) {
        ex.printStackTrace();
    }
  }

  //发送密码报文
  private void sendPIN(String password,Socket serverSocket) {
    try {

      // 创建输入流和输出流
      BufferedReader reader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
      PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);

      // 发送报文
      out.println(password+ '\n');

      // 接收服务器的响应报文
      String response = reader.readLine();
      System.out.println("服务器响应：" + response);

      // 处理服务器的响应报文
      if (response.equals("525 OK!")) {
        cardLayout.show(cardPanel, "menu");
      } else if(response.equals("401 ERROR!")){
        // 弹窗警告检查卡号是否正确
        JOptionPane.showMessageDialog(frame, "密码错误！", "警告", JOptionPane.ERROR_MESSAGE);
      } else{
        JOptionPane.showMessageDialog(frame, "未知错误，可能是密码为空！", "警告", JOptionPane.WARNING_MESSAGE);
      }

    } catch (IOException ex) {
        ex.printStackTrace();
    }
  }

  //发送取款报文
  private void sendWDRA(String password,Socket serverSocket) {
    try {

      // 创建输入流和输出流
      BufferedReader reader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
      PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);

      // 发送报文
      out.println(password+ '\n');

      // 接收服务器的响应报文
      String response = reader.readLine();
      System.out.println("服务器响应：" + response);

      // 处理服务器的响应报文
      if (response.equals("525 OK!")) {
        //弹窗显示取款成功
        JOptionPane.showMessageDialog(frame, "取款成功", "提示", JOptionPane.INFORMATION_MESSAGE);
        //cardLayout.show(cardPanel, "menu");
      } else {
          // 弹窗警告检查卡号是否正确
          JOptionPane.showMessageDialog(frame, "余额不足！", "警告", JOptionPane.WARNING_MESSAGE);
      }

      // 关闭连接
      //serverSocket.close();
    } catch (IOException ex) {
        ex.printStackTrace();
    }
  }

  //发送BYE报文
  private void sendBYE(String password,Socket serverSocket,Frame frame){
    try {

      // 创建输入流和输出流
      BufferedReader reader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
      PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);

      // 发送报文
      out.println(password+ '\n');

      // 接收服务器的响应报文
      String response = reader.readLine();
      System.out.println("服务器响应：" + response);

      // 处理服务器的响应报文
      if (response.equals("BYE")) {
        serverSocket.close();
        JOptionPane.showMessageDialog(frame, "退卡成功", "提示", JOptionPane.INFORMATION_MESSAGE);
        frame.dispose();
        System.exit(0);
        // cardLayout.show(cardPanel, "insertCard");
        // cardTextField.setText("");

      } else {
          // 弹窗警告检查卡号是否正确
          JOptionPane.showMessageDialog(frame, "请检查密码是否正确！", "警告", JOptionPane.WARNING_MESSAGE);
      }
    } catch (IOException ex) {
        ex.printStackTrace();
    }
  }

  //发送BALA报文
  private String sendBALA(String password,Socket serverSocket){
    try {

      // 创建输入流和输出流
      BufferedReader reader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
      PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);

      // 发送报文
      out.println(password+ '\n');

      // 接收服务器的响应报文
      String response = reader.readLine();
      System.out.println("服务器响应：" + response);

      // 处理服务器的响应报文
      String response_Server[]=response.split(":");

      if (response_Server[0].equals("AMNT")) {
        //JOptionPane.showMessageDialog(frame, "退卡成功", "提示", JOptionPane.INFORMATION_MESSAGE);
        cardLayout.show(cardPanel, "balance");
        return response_Server[1];
        // cardTextField.setText("");

      } else {
          // 弹窗警告检查卡号是否正确
          JOptionPane.showMessageDialog(frame, "请检查密码是否正确！", "警告", JOptionPane.WARNING_MESSAGE);
      }
    } catch (IOException ex) {
        ex.printStackTrace();
    }
    return "null";
  }

  //创建连接
  private Socket connectToServer() {
    try {
        // 连接服务器
        //192.168.185.68 , 12345 xiongfeng
        //192.168.247.34 , 2525 lijiaqing
        clientSocket = new Socket("10.242.228.10", 2525);
        //PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        return clientSocket;
    } catch (IOException ex) {
        ex.printStackTrace();
    }
    return null;
  }

}