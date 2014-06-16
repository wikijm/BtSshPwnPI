package com.cloned.hackdroid;


import android.os.Environment;

import com.jcraft.jsch.*;
import java.io.*;

/**
 *
 * @author ayokunle
 */

public class ServerIO {

public static void main(String[] arg){

ServerIO x = new ServerIO();

}

boolean getFile(String [] arg, String UserDetails){
    if(arg.length!=2){
        System.err.println("usage: java ScpFrom user@remotehost:file1 file2");
        System.exit(-1);
    }      
    FileOutputStream fos=null;
        try{
            String user=arg[0].substring(0, arg[0].indexOf('@'));
            arg[0]=arg[0].substring(arg[0].indexOf('@')+1);
            String host=arg[0].substring(0, arg[0].indexOf(':'));
            String rfile="webspace/Shield/" + UserDetails;
            String lfile= UserDetails;

            String prefix=null;
            File root = new File(Environment.getExternalStorageDirectory(), "Notes");

            if(new File(root, lfile).isDirectory()){
                prefix=lfile+File.separator;
            }

            JSch jsch=new JSch();
            Session session=jsch.getSession(user, host, 22);

            // username and password will be given via UserInfo interface.
            UserInfo ui=new MyUserInfo();
            session.setUserInfo(ui);
            session.connect();

            // exec 'scp -f rfile' remotely
            String command="scp -f "+rfile;
            Channel channel=session.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);

            // get I/O streams for remote scp
            OutputStream out=channel.getOutputStream();
            InputStream in=channel.getInputStream();

            channel.connect();

            byte[] buf=new byte[1024];

            // send '\0'
            buf[0]=0; out.write(buf, 0, 1); out.flush();

            while(true){
                int c=checkAck(in);
                if(c!='C'){
                    break;
                }

                // read '0644 '
                in.read(buf, 0, 5);

                long filesize=0L;
                while(true){
                    if(in.read(buf, 0, 1)<0){
                    // error
                        break; 
                    }
                    if(buf[0]==' ')break;
                        filesize=filesize*10L+(long)(buf[0]-'0');
                    }

                    String file=null;
                    for(int i=0;;i++){
                        in.read(buf, i, 1);
                        if(buf[i]==(byte)0x0a){
                            file=new String(buf, 0, i);
                            break;
                        }
                    }


                    // send '\0'
                    buf[0]=0; out.write(buf, 0, 1); out.flush();

                    // read a content of lfile
                    fos=new FileOutputStream(prefix==null ? lfile : prefix+file);
                    int foo;
                    while(true){
                        if(buf.length<filesize) foo=buf.length;
                        else foo=(int)filesize;
                        foo=in.read(buf, 0, foo); 
                        if(foo<0){
                            // error 
                            break;
                        }
                        fos.write(buf, 0, foo);
                        filesize-=foo;
                        if(filesize==0L) break;
                    }
                    fos.close();
                    fos=null;

                    if(checkAck(in)!=0){
                        return false;
                    }

                    // send '\0'
                    buf[0]=0; out.write(buf, 0, 1); out.flush();
            }

            session.disconnect();

            return true;
        }catch(Exception e){
            System.out.println(e);
            try{
                if(fos!=null)
                    fos.close();
                }catch(Exception ee){

                }
            return false;
        }
}

static int checkAck(InputStream in) throws IOException{
int b=in.read();
// b may be 0 for success,
//          1 for error,
//          2 for fatal error,
//          -1
if(b==0) return b;
if(b==-1) return b;

if(b==1 || b==2){
  StringBuffer sb=new StringBuffer();
  int c;
  do {
c=in.read();
sb.append((char)c);
  }
  while(c!='\n');
  if(b==1){ // error
System.out.print(sb.toString());
  }
  if(b==2){ // fatal error
System.out.print(sb.toString());
  }
}
return b;
}

public static class MyUserInfo implements UserInfo, UIKeyboardInteractive{
    public String getPassword(){ return passwd; }
    public boolean promptYesNo(String str){
       return true;
    }

    String passwd = "Adegoke01";

    public String getPassphrase(){ return null; }
    public boolean promptPassphrase(String message){ 
        return true; 
    }

    public boolean promptPassword(String message){
      return true;
    }

    public void showMessage(String message){

    }

    public String[] promptKeyboardInteractive(String destination,
                                              String name,
                                              String instruction,
                                              String[] prompt,
                                              boolean[] echo){

      String [] response = new String[2];
      response[0] = "username";
      response[1] = "password";
      return response;
    }
  }

String sendFile(String [] arg , String UserDetails){

    if(arg.length!=2){
        System.err.println("usage: java ScpTo file1 user@remotehost:file2");
        System.exit(-1);
    }      

FileInputStream fis=null;
try{     
  String user=arg[0].substring(0, arg[0].indexOf('@'));
  arg[0]=arg[0].substring(arg[0].indexOf('@')+1);
  String host=arg[0].substring(0, arg[0].indexOf(':'));
  String rfile="webspace/Shield/" + UserDetails;
  String lfile= UserDetails;        

  JSch jsch=new JSch();
  Session session=jsch.getSession(user, host, 22);

  // username and password will be given via UserInfo interface.
  UserInfo ui=new MyUserInfo();
  session.setUserInfo(ui);
  session.connect();

  boolean ptimestamp = true;

  // exec 'scp -t rfile' remotely
  String command="scp " + (ptimestamp ? "-p" :"") +" -t "+rfile;
  Channel channel=session.openChannel("exec");
  ((ChannelExec)channel).setCommand(command);

  // get I/O streams for remote scp
  OutputStream out=channel.getOutputStream();
  InputStream in=channel.getInputStream();

  channel.connect();

  if(checkAck(in)!=0){
      return "Possible error 1";
  }

  File root = Environment.getExternalStorageDirectory();
  File _lfile = new File(root, lfile);

  if(ptimestamp){
    command="T "+(_lfile.lastModified()/1000)+" 0";
    // The access time should be sent here,
    // but it is not accessible with JavaAPI ;-<
    command+=(" "+(_lfile.lastModified()/1000)+" 0\n"); 
    out.write(command.getBytes()); out.flush();
    if(checkAck(in)!=0){
        return "Possible error 2";
    }
  }

  // send "C0644 filesize filename", where filename should not include '/'
  long filesize=_lfile.length();
  command="C0644 "+filesize+" ";
  if(lfile.lastIndexOf('/')>0){
    command+=lfile.substring(lfile.lastIndexOf('/')+1);
  }
  else{
    command+=lfile;
  }
  command+="\n";
  out.write(command.getBytes()); out.flush();
  if(checkAck(in)!=0){
      return "Possible error 3";
  }
  //here
  // send a content of lfile
  fis=new FileInputStream(_lfile);
  byte[] buf=new byte[1024];
  while(true){
    int len=fis.read(buf, 0, buf.length);
if(len<=0) break;
    out.write(buf, 0, len); //out.flush();
  }
  fis.close();
  fis=null;

  // send '\0'
  buf[0]=0; out.write(buf, 0, 1); out.flush();
  if(checkAck(in)!=0){
      return "Possible error 4";
  }
  out.close();

  channel.disconnect();
  session.disconnect();

  return "No errors";
}
catch(Exception e){
  System.out.println(e);
  try{if(fis!=null)fis.close();}catch(Exception ee){}

  return Thread.currentThread().getStackTrace().toString();
}
}
}