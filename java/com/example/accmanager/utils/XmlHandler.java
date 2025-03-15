package com.example.accmanager.utils;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlHandler
{
    // parse
    private static DocumentBuilderFactory DocBuilderFactory;
    private static DocumentBuilder DocBuilder;
    private static Document Doc;
    // write
    private static TransformerFactory TfFactory;
    private static Transformer Tf;
    private static DOMSource Source;
    private static StreamResult Result;
    // file
    private static File SaveFile;
    private static Node RootTag, AccsTag;
    // tag names
    private static String rootTagName, lockCodeTagName, accsTagName, accTagName, accNameTagName, extraInfoTagName, pwdTagName;
    // looper

    public static void Init()
    {
        // tag names
        rootTagName = "root";
        lockCodeTagName = "lock-code";
        accsTagName = "accounts";
        accTagName = "account";
        accNameTagName = "account-name";
        extraInfoTagName = "account-extra-info";
        pwdTagName = "account-password";
        // save file
        SaveFile = new File("data/data/com.example.accmanager/save.xml");
        // parse & write
        try {
            DocBuilderFactory = DocumentBuilderFactory.newInstance();
            DocBuilder = DocBuilderFactory.newDocumentBuilder();
            TfFactory = TransformerFactory.newInstance();
            Tf = TfFactory.newTransformer();
        }
        catch (Exception e)
        {
            System.out.println("Parse & Write Exception: " + e);
        }
        // create save file if not exists
        try
        {
    //        SaveFile.delete();
            if (!SaveFile.exists())
            {
                System.out.println("Save file doesn't exist");
                // create save file
                boolean ok = SaveFile.createNewFile();
                if (!ok)
                {
                    System.out.println("Failed to create save file");
                    return;
                }
                System.out.println("Created save file");
                Doc = DocBuilder.newDocument();
                Result = new StreamResult(SaveFile);
                // root tag
                RootTag = Doc.createElement(rootTagName);
                Doc.appendChild(RootTag);
                // security section tag
                Node securitySectionTag = Doc.createElement("security-section");
                Node lockCodeTag = Doc.createElement("lock-code");
                securitySectionTag.appendChild(lockCodeTag);
                RootTag.appendChild(securitySectionTag);
                // accs tag
                AccsTag = Doc.createElement(accsTagName);
                RootTag.appendChild(AccsTag);
                // commit changes
                Commit();
            }
            else
            {
                Doc = DocBuilder.parse(SaveFile);
                RootTag = Doc.getElementsByTagName(rootTagName).item(0);
                AccsTag = Doc.getElementsByTagName(accsTagName).item(0);
                Result = new StreamResult(SaveFile);
            }
        }
        catch (Exception e)
        {
            System.out.println("Create Exception: " + e);
        }
    }

    public static void SaveAcc(ArrayList<String> EncAccDetails)
    {
        Node AccTag = Doc.createElement(accTagName);
        Node AccNameTag = Doc.createElement(accNameTagName);
        AccNameTag.setTextContent(EncAccDetails.get(0));
        Node ExtraInfoTag = Doc.createElement(extraInfoTagName);
        ExtraInfoTag.setTextContent(EncAccDetails.get(1));
        Node PwdTag = Doc.createElement(pwdTagName);
        PwdTag.setTextContent(EncAccDetails.get(2));
        AccTag.appendChild(AccNameTag);
        AccTag.appendChild(ExtraInfoTag);
        AccTag.appendChild(PwdTag);
        AccsTag.appendChild(AccTag);
        // commit changes
        Commit();
        // update on activity
        ArrayList<String> AccDetails = new ArrayList<>();
        AccDetails.add(Cryptor.Decrypt(EncAccDetails.get(0)));
        AccDetails.add(Cryptor.Decrypt(EncAccDetails.get(1)));
        AccDetails.add(Cryptor.Decrypt(EncAccDetails.get(2)));
        ViewHandler.MainActivityRef.AddTableRow(AccDetails, -1);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void UpdateAcc(ArrayList<Object> Msg)
    {
        // table has 1 more row for the header
        String prevAccName = Cryptor.Decrypt(String.valueOf(Msg.remove(0)));
        // device does not have this account
        int allAccsIndex = XmlHandler.GetAccNames().indexOf(prevAccName);
        if (allAccsIndex == -1)
        {
            // create it instead
            System.out.println("Device does not have this account. Replacing the msg on this device with a create msg instead");
            ArrayList<String> EncAccDetails = (ArrayList<String>)Msg.remove(0);
            SaveAcc(EncAccDetails);
            return;
        }
        // remove latest details arr
        // only if its a broadcast msg, I am using it locally as well to speed up table update delay
        if (((ArrayList<String>)Msg.get(0)).size() == 3)
        {
            Msg.remove(0);
        }
        // update xml
        NodeList AccChildTags = AccsTag.getChildNodes().item(allAccsIndex).getChildNodes();
        for (int i = 0; i < Msg.size(); i++)
        {
            ArrayList<String> EncUpdatedAccDetailArr = (ArrayList<String>)Msg.get(i);
            int updatedIndex = Integer.parseInt(Cryptor.Decrypt(EncUpdatedAccDetailArr.get(0)));
            String encUpdatedValue = EncUpdatedAccDetailArr.get(1);
            AccChildTags.item(updatedIndex).setTextContent(encUpdatedValue);
        }
        // commit changes
        Commit();
        // update on activity
        Msg.add(0, prevAccName);
        ViewHandler.MainActivityRef.UpdateTableRow(Msg, allAccsIndex);
    }
    public static void DelAcc(String encAccName)
    {
        // decrypt acc name
        String accName = Cryptor.Decrypt(encAccName);
        System.out.println(accName);
        int accIndex = GetAccNames().indexOf(accName);
        if (accIndex == -1)
        {
            System.out.println("Device does not have this account. Skipping...");
            return;
        }
        Node Acc = AccsTag.getChildNodes().item(accIndex);
        AccsTag.removeChild(Acc);
        // commit changes
        Commit();
        // update acc table
        ViewHandler.MainActivityRef.RemoveTableRow(accName,-0, accIndex);
    }

    public static String GetLockCode()
    {
        String code = Cryptor.Decrypt(Doc.getElementsByTagName(lockCodeTagName).item(0).getTextContent());
        return code;
    }
    public static boolean IsLocked()
    {
        return GetLockCode().length() > 0;
    }

    public static ArrayList<ArrayList<String>> GetAccs(boolean decrypt)
    {
        NodeList EncAccs = Doc.getElementsByTagName(accTagName);
        ArrayList<ArrayList<String>> Accs = new ArrayList<>();
        for (int i = 0; i < EncAccs.getLength(); i++)
        {
            NodeList EncAccDetails = EncAccs.item(i).getChildNodes();
            String accNameText = EncAccDetails.item(0).getTextContent();
            String extraInfoText = EncAccDetails.item(1).getTextContent();
            String pwdText = EncAccDetails.item(2).getTextContent();
            // decrypt
            if (decrypt)
            {
                accNameText = Cryptor.Decrypt(accNameText);
                extraInfoText = Cryptor.Decrypt(extraInfoText);
                pwdText = Cryptor.Decrypt(pwdText);
            }
            ArrayList<String> Acc = new ArrayList<>(Arrays.asList(accNameText, extraInfoText, pwdText));
            Accs.add(Acc);
        }
        return Accs;
    }
    public static ArrayList<String> GetAccNames()
    {
        NodeList AccNameTags = Doc.getElementsByTagName(accNameTagName);
        ArrayList<String> AccNames = new ArrayList<>();
        for (int i = 0; i < AccNameTags.getLength(); i++)
        {
            Node AccNameTag = AccNameTags.item(i);
            String accName = Cryptor.Decrypt(AccNameTag.getTextContent());
            AccNames.add(accName);
        }
        return AccNames;
    }
    public static ArrayList<String> GetPwds()
    {
        NodeList PwdTags = Doc.getElementsByTagName(pwdTagName);
        ArrayList<String> Pwds = new ArrayList<>();
        for (int i = 0; i < PwdTags.getLength(); i++)
        {
            Node PwdTag = PwdTags.item(i);
            String pwd = Cryptor.Decrypt(PwdTag.getTextContent());
            Pwds.add(pwd);
        }
        return Pwds;
    }

    private static void Commit()
    {
        try
        {
            Source = new DOMSource(Doc);
            Tf.transform(Source, Result);
        }
        catch (Exception e)
        {
            System.out.println("Commit Exception: " + e);
        }
    }
    
    public static String[] GetConnValues() 
    {
        try
        {
            DocumentBuilderFactory DocBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder DocBuilder = DocBuilderFactory.newDocumentBuilder();
            // Load connection xml file from the project resources folder
            InputStream inputStream = XmlHandler.class.getClassLoader().getResourceAsStream("conn.xml");
            if (inputStream == null)
            {
                return null;
            }
            Document ConnXmlDoc = DocBuilder.parse(inputStream);
            String serverPrivateIp = ConnXmlDoc.getElementsByTagName("server-private-ip").item(0).getTextContent();
            String hostname = ConnXmlDoc.getElementsByTagName("hostname").item(0).getTextContent();
            String portStr = ConnXmlDoc.getElementsByTagName("port").item(0).getTextContent();
            return new String[] { serverPrivateIp, hostname, portStr };
        }
        catch (Exception e)
        {
            System.out.println("Parse conn xml file exception: " + e);
            return null;
        }
    }
}
