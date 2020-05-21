package utils;

public class HexUtils {
    private static final String HEX_CHARSET = "0123456789ABCDEF";
    private static final String BLANK = " ";
    /**
     * 方法功能：将十六进制字符串转换为字节数组byte[]。
     * @param  hexString
     * @return byte[]
     * */
    public static byte[] Hex2Bytes(String hexString){
        byte[] arrB = hexString.getBytes();
        int iLen = arrB.length;
        byte[] arrOut = new byte[iLen / 2];
        String strTmp = null;
        for (int i = 0; i < iLen; i += 2)
        {
            strTmp = new String(arrB, i, 2);
            arrOut[(i / 2)] = ((byte)Integer.parseInt(strTmp, 16));
        }
        return arrOut;
    }
//    public static void main(String[] args) throws UnsupportedEncodingException {
//        //测试方法Hex2Bytes(String hexString)
//        String hexString = "61616161E4B8ADE59BBDE58AA0E6B2B9EFBC8CE68898E8839CE696B0E586A0E78AB6E79785E6AF92";
//        byte[]b = Hex2Bytes(hexString);
//        System.out.println("字节数组byte[] =\n"+ Arrays.toString(b));
//        String str = new String(b,"utf-8");
//        System.out.println("字节数转换为字符串：\n"+str);
//    }
}
