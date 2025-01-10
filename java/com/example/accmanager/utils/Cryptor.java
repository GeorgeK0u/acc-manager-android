package com.example.accmanager.utils;

public class Cryptor
{
    private static int wrongChars;
    private static char maskCh;

    public static void Init()
    {
        wrongChars = 5;
        maskCh = '*';
    }

    public static String Encrypt(String text)
    {
        String encryptedText = "";
        for (int i = 0; i < text.length(); i++)
        {
            int ascii = (int)text.charAt(i);
            encryptedText += (char)(ascii+wrongChars);
        }
        return encryptedText;
    }

    public static String Decrypt(String text)
    {
        String decryptedText = "";
        for (int i = 0; i < text.length(); i++)
        {
            int ascii = (int)text.charAt(i);
            decryptedText += (char)(ascii-wrongChars);
        }
        return decryptedText;
    }

    public static String Mask(String pwd)
    {
        String maskedPwd = "";
        for (int i = 0; i < pwd.length(); i++)
        {
            maskedPwd += maskCh;
        }
        return maskedPwd;
    }
}