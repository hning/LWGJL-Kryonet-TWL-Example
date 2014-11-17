/*
 * Copyright (c) 2008-2010, Matthias Mann
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package chat;

import java.awt.EventQueue;
import java.io.IOException;

import javax.swing.DefaultListModel;

import de.matthiasmann.twl.DesktopArea;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.FPSCounter;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.ResizableFrame;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.TextArea;
import de.matthiasmann.twl.textarea.HTMLTextAreaModel;
import de.matthiasmann.twl.renderer.lwjgl.LWJGLRenderer;
import de.matthiasmann.twl.theme.ThemeManager;

import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

import test.TestUtils;
import chat.Network.ChatMessage;
import chat.Network.RegisterName;
import chat.Network.UpdateNames;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;


/**
 * A chat demo + Kryonet
 *
 * This class also acts as root pane
 */
public class ChatDemo extends DesktopArea{
  public Client client;
  private final FPSCounter fpsCounter;
  private final ChatFrame chatFrame;
  public boolean quit;
  public String name;
  
  public ChatDemo()  {
      client = new Client();
      client.start();
      Network.register(client);
      
      name = "test";
      
      client.addListener(new Listener() {
        public void connected (Connection connection) {
          RegisterName registerName = new RegisterName();
          registerName.name = name;
          client.sendTCP(registerName);
        }

        public void received (Connection connection, Object object) {
//          if (object instanceof UpdateNames) {
//            UpdateNames updateNames = (UpdateNames)object;
//            chatFrame.setNames(updateNames.names);
//            return;
//          }

          if (object instanceof ChatMessage) {
            ChatMessage chatMessage = (ChatMessage)object;
            System.out.println(chatMessage.text);
            chatFrame.addRowThreadsafe(chatMessage.text);
            return;
          }
        }

//        public void disconnected (Connection connection) {
//          EventQueue.invokeLater(new Runnable() {
//            public void run () {
//              // Closing the frame calls the close listener which will stop the client's update thread.
//              chatFrame.dispose();
//            }
//          });
//        }
      });
      
      fpsCounter = new FPSCounter();
      add(fpsCounter);

      chatFrame = new ChatFrame(this);
      add(chatFrame);

      chatFrame.setSize(400, 200);
      chatFrame.setPosition(10, 350);
      
      
      final String host = "localhost";
      
      try {
          Display.setDisplayMode(new DisplayMode(800, 600));
          Display.create();
          Display.setTitle("TWL Chat Demo");
          Display.setVSyncEnabled(true);

          LWJGLRenderer renderer = new LWJGLRenderer();
//          ChatDemo chat = new ChatDemo();
          GUI gui = new GUI(this, renderer);

          ThemeManager theme = ThemeManager.createThemeManager(
                  ChatDemo.class.getResource("chat.xml"), renderer);
          gui.applyTheme(theme);
          
          client.connect(5000, host, Network.port);
//          new Thread("Connect") {
//            public void run () {
//              try {
//                client.connect(5000, host, Network.port);
//                // Server communication after connection can go here, or in Listener#connected().
//              } catch (IOException ex) {
//                ex.printStackTrace();
//                System.exit(1);
//              }
//            }
//          }.start();

          while(!Display.isCloseRequested() && !this.quit) {
              GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

              gui.update();
              Display.update();
              TestUtils.reduceInputLag();
          }

          gui.destroy();
          theme.destroy();
      } catch (Exception ex) {
          TestUtils.showErrMsg(ex);
      }
      Display.destroy();
  }
  
  static class ChatFrame extends ResizableFrame {
    private final StringBuilder sb;
    private final HTMLTextAreaModel textAreaModel;
    private final TextArea textArea;
    private final EditField editField;
    private final ScrollPane scrollPane;
    private int curColor;
    private de.matthiasmann.twl.GUI gui;

    public ChatFrame(final ChatDemo outerFrame) {
        setTitle("Chat");

        this.sb = new StringBuilder();
        this.textAreaModel = new HTMLTextAreaModel();
        this.textArea = new TextArea(textAreaModel);
        this.editField = new EditField();

        editField.addCallback(new EditField.Callback() {
            public void callback(int key) {
                if(key == Event.KEY_RETURN) {
                    // cycle through 3 different colors/font styles
//                    appendRow("color"+curColor, editField.getText());
//                    editField.setText("");
//                    curColor = (curColor + 1) % 3;
                                      
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.text = editField.getText();
                    outerFrame.client.sendTCP(chatMessage);
                    editField.setText("");
                }
            }
        });

        textArea.addCallback(new TextArea.Callback() {
            public void handleLinkClicked(String href) {
                Sys.openURL(href);
            }
        });

        scrollPane = new ScrollPane(textArea);
        scrollPane.setFixed(ScrollPane.Fixed.HORIZONTAL);

        DialogLayout l = new DialogLayout();
        l.setTheme("content");
        l.setHorizontalGroup(l.createParallelGroup(scrollPane, editField));
        l.setVerticalGroup(l.createSequentialGroup(scrollPane, editField));

        add(l);

        appendRow("default", "Welcome to the chat demo. Type your messages below :)");
    }

    private void appendRow(String font, String text) {
        sb.append("<div style=\"word-wrap: break-word; font-family: ").append(font).append("; \">");
        // not efficient but simple
        for(int i=0,l=text.length() ; i<l ; i++) {
            char ch = text.charAt(i);
            switch(ch) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '&': sb.append("&amp;"); break;
                case '"': sb.append("&quot;"); break;
                case ':':
                    if(text.startsWith(":)", i)) {
                        sb.append("<img src=\"smiley\" alt=\":)\"/>");
                        i += 1; // skip one less because of i++ in the for loop
                        break;
                    }
                    sb.append(ch);
                    break;
                case 'h':
                    if(text.startsWith("http://", i)) {
                        int end = i + 7;
                        while(end < l && isURLChar(text.charAt(end))) {
                            end++;
                        }
                        String href = text.substring(i, end);
                        sb.append("<a style=\"font: link\" href=\"").append(href)
                                .append("\" >").append(href)
                                .append("</a>");
                        i = end - 1; // skip one less because of i++ in the for loop
                        break;
                    }
                    // fall through:
                default:
                    sb.append(ch);
            }
        }
        sb.append("</div>");

        boolean isAtEnd = scrollPane.getMaxScrollPosY() == scrollPane.getScrollPositionY();

        textAreaModel.setHtml(sb.toString());

        if(isAtEnd) {
            scrollPane.validateLayout();
            scrollPane.setScrollPositionY(scrollPane.getMaxScrollPosY());
        }
    }

    private boolean isURLChar(char ch) {
        return (ch == '.') || (ch == '/') || (ch == '%') ||
                (ch >= '0' && ch <= '9') ||
                (ch >= 'a' && ch <= 'z') ||
                (ch >= 'A' && ch <= 'Z');
    }
    
    public void addMessage(final String message){
      appendRow("color"+curColor, message);
    }
    
    @Override
    protected void afterAddToGUI(de.matthiasmann.twl.GUI gui) {
      this.gui = gui;
    }
    
    private synchronized void addRowThreadsafe(final String msg) {
      if(this.gui != null) {
         this.gui.invokeLater(new Runnable() {
            public void run() {
               addMessage(msg);
            }
         }
      );
      }
    }
}
  
  @Override
  protected void layout() {
      super.layout();

      // fpsCounter is bottom right
      fpsCounter.adjustSize();
      fpsCounter.setPosition(
              getInnerWidth() - fpsCounter.getWidth(),
              getInnerHeight() - fpsCounter.getHeight());
  }

  @Override
  protected boolean handleEvent(Event evt) {
      if(super.handleEvent(evt)) {
          return true;
      }
      switch (evt.getType()) {
          case KEY_PRESSED:
              switch (evt.getKeyCode()) {
                  case Event.KEY_ESCAPE:
                      quit = true;
                      return true;
              }
      }
      return false;
  }
  
  public static void main (String[] args) {
    Log.set(Log.LEVEL_DEBUG);
    new ChatDemo();
  }
}

