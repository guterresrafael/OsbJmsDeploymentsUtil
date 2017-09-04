import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class OsbJmsDeploymentsUtil
{
  private static HashMap<String, String> DEPLOYMENTS = new HashMap();
  private String configFileName;
  private static String EJBNAME = null;
  private static String QUEUENAME = null;
  private static String REFNAME = null;
  private static String APP_TARGET = null;
  private static String EAR_NAME = null;
  private static PrintWriter WRITER = null;
  private static final String WLS_APP_DESC = "META-INF/weblogic-application.xml";
  private static final String WLS_EJB_DESC = "META-INF/weblogic-ejb-jar.xml";
  private static final String APP_PARAM = "application-param";
  private static final String PARAM_NAME = "param-name";
  private static final String PARAM_VAL = "param-value";
  private static final String EJB_JAR = "ejb.jar";
  private static final String EJB_NAME = "ejb-name";
  private static final String RES_REF_NAME = "res-ref-name";
  private static final String RES_DESC = "resource-description";
  private static final String JNDI_NAME = "jndi-name";
  private static final String SERVICE_REF = "service-ref";
  private static final String QUEUE_NAME = "jms/QueueName";
  private static final String TOPIC_NAME = "jms/TopicName";
  private static final String APP_DEP = "app-deployment";
  private static final String NAME = "name";
  private static final String TARGET = "target";
  private static final String EJB = "weblogic-enterprise-bean";
  private static XMLInputFactory xmlif = null;
  
  static
  {
    try
    {
      xmlif = XMLInputFactory.newInstance();
      xmlif.setProperty("javax.xml.stream.isReplacingEntityReferences", Boolean.TRUE);
      
      xmlif.setProperty("javax.xml.stream.isSupportingExternalEntities", Boolean.FALSE);
      
      xmlif.setProperty("javax.xml.stream.isCoalescing", Boolean.FALSE);
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
    }
  }
  
  public static void main(String[] paramArrayOfString)
  {
    OsbJmsDeploymentsUtil localOSBJMSDeploymentsUtil = new OsbJmsDeploymentsUtil();
    localOSBJMSDeploymentsUtil.list(paramArrayOfString);
  }
  
  public void list(String[] paramArrayOfString)
  {
    if (paramArrayOfString.length != 3)
    {
      System.out.println("Usage java OsbJmsDeploymentsUtil sbgendir config_xml_file outfile\n  i.e java OsbJmsDeploymentsUtil /MW_HOME/user_projects/domains/OSBDomain/sbgen /MW_HOME/user_projects/domains/OSBDomain/config/config.xml util.out");
      System.exit(-1);
    }
    this.configFileName = paramArrayOfString[1];
    try
    {
      processConfigFile(this.configFileName);
      File localFile = new File(paramArrayOfString[0]);
      if (!localFile.isDirectory())
      {
        System.out.println("Argument is not a directory");
        System.exit(-1);
      }
      WRITER = new PrintWriter(paramArrayOfString[2]);
      
      WRITER.println("EAR File,Service Ref,Destination Name,EJB Name,Target");
      processDirectory(localFile);
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
    }
    finally
    {
      if (WRITER != null) {
        WRITER.close();
      }
    }
  }
  
  private void processDirectory(File paramFile)
    throws Exception
  {
    String[] arrayOfString = paramFile.list();
    for (int i = 0; i < arrayOfString.length; i++)
    {
      File localFile = new File(paramFile.getPath(), arrayOfString[i]);
      if ((localFile.getName().startsWith("_ALSB_") || localFile.getName().startsWith("SB_")) && (localFile.getName().endsWith(".ear"))) {
        processArchive(localFile);
      }
    }
  }
  
  private void processArchive(File paramFile)
    throws Exception
  {
    JarInputStream localJarInputStream = new JarInputStream(new FileInputStream(paramFile));
    ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream(10240);
    
    byte[] arrayOfByte1 = null;
    byte[] arrayOfByte2 = new byte['?'];
    ByteArrayInputStream localByteArrayInputStream1 = null;
    ByteArrayInputStream localByteArrayInputStream2 = null;
    JarEntry localJarEntry;
    while ((localJarEntry = localJarInputStream.getNextJarEntry()) != null)
    {
      int i;
      if (localJarEntry.getName().equals("META-INF/weblogic-application.xml"))
      {
        i = 0;
        while (i != -1)
        {
          i = localJarInputStream.read(arrayOfByte2, 0, arrayOfByte2.length);
          if (i != -1)
          {
            localByteArrayOutputStream.write(arrayOfByte2, 0, i);
          }
          else
          {
            localByteArrayOutputStream.flush();
            arrayOfByte1 = localByteArrayOutputStream.toByteArray();
          }
        }
        localByteArrayInputStream2 = new ByteArrayInputStream(arrayOfByte1);
        localByteArrayOutputStream.reset();
      }
      else if (localJarEntry.getName().equals("ejb.jar"))
      {
        i = 0;
        while (i != -1)
        {
          i = localJarInputStream.read(arrayOfByte2, 0, arrayOfByte2.length);
          if (i != -1)
          {
            localByteArrayOutputStream.write(arrayOfByte2, 0, i);
          }
          else
          {
            localByteArrayOutputStream.flush();
            arrayOfByte1 = localByteArrayOutputStream.toByteArray();
          }
        }
        localByteArrayInputStream1 = new ByteArrayInputStream(arrayOfByte1);
        localByteArrayOutputStream.reset();
      }
    }
    EAR_NAME = paramFile.getName();
    String str = EAR_NAME.substring(0, EAR_NAME.indexOf(".ear"));
    APP_TARGET = null;
    if (DEPLOYMENTS.containsKey(str)) {
      APP_TARGET = (String)DEPLOYMENTS.get(str);
    }
    processFile(EAR_NAME, localByteArrayInputStream2);
    localByteArrayInputStream2.close();
    processEJBArchive(paramFile.getName(), localByteArrayInputStream1);
    localByteArrayInputStream1.close();
  }
  
  public static void processFile(String paramString, InputStream paramInputStream)
    throws Exception
  {
    try
    {
      XMLStreamReader localXMLStreamReader = xmlif.createXMLStreamReader(paramInputStream);
      
      int j = 0;
      while (localXMLStreamReader.hasNext())
      {
        int i = localXMLStreamReader.next();
        String str1;
        if (1 == i)
        {
          str1 = localXMLStreamReader.getLocalName();
          if ("param-name".equals(str1))
          {
            String str2 = localXMLStreamReader.getElementText();
            if ("service-ref".equals(str2)) {
              j = 1;
            }
          }
          else if (("param-value".equals(str1)) && 
            (j != 0))
          {
            REFNAME = localXMLStreamReader.getElementText();
          }
        }
        else if (2 == i)
        {
          str1 = localXMLStreamReader.getLocalName();
          if ("application-param".equals(str1)) {
            j = 0;
          }
        }
      }
    }
    catch (XMLStreamException localXMLStreamException)
    {
      System.out.println(localXMLStreamException.getMessage());
      if (localXMLStreamException.getNestedException() != null) {
        localXMLStreamException.getNestedException().printStackTrace();
      }
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
    }
  }
  
  public static void processEJBArchive(String paramString, InputStream paramInputStream)
    throws Exception
  {
    System.err.println("Processing EAR: " + paramString);
    JarInputStream localJarInputStream = new JarInputStream(paramInputStream);
    
    ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream(10240);
    
    byte[] arrayOfByte1 = null;
    byte[] arrayOfByte2 = new byte['?'];
    JarEntry localJarEntry;
    while ((localJarEntry = localJarInputStream.getNextJarEntry()) != null) {
      if (localJarEntry.getName().equals("META-INF/weblogic-ejb-jar.xml"))
      {
        int i = 0;
        while (i != -1)
        {
          i = localJarInputStream.read(arrayOfByte2, 0, arrayOfByte2.length);
          if (i != -1)
          {
            localByteArrayOutputStream.write(arrayOfByte2, 0, i);
          }
          else
          {
            localByteArrayOutputStream.flush();
            arrayOfByte1 = localByteArrayOutputStream.toByteArray();
          }
        }
        ByteArrayInputStream localByteArrayInputStream = new ByteArrayInputStream(arrayOfByte1);
        processWeblogicEJBFile(paramString, localByteArrayInputStream);
        localByteArrayInputStream.close();
        localByteArrayOutputStream.reset();
      }
    }
  }
  
  public static void processWeblogicEJBFile(String paramString, InputStream paramInputStream)
    throws Exception
  {
    try
    {
      XMLStreamReader localXMLStreamReader = xmlif.createXMLStreamReader(paramInputStream);
      
      int j = 0;
      
      Object localObject = null;
      while (localXMLStreamReader.hasNext())
      {
        int i = localXMLStreamReader.next();
        String str1;
        if (1 == i)
        {
          str1 = localXMLStreamReader.getLocalName();
          if ("res-ref-name".equals(str1))
          {
            String str2 = localXMLStreamReader.getElementText();
            if (("jms/QueueName".equals(str2)) || ("jms/TopicName".equals(str2))) {
              j = 1;
            }
          }
          else if ("jndi-name".equals(str1))
          {
            if (j != 0) {
              QUEUENAME = localXMLStreamReader.getElementText();
            }
          }
          else if ("ejb-name".equals(str1))
          {
            EJBNAME = localXMLStreamReader.getElementText();
          }
        }
        else if (2 == i)
        {
          str1 = localXMLStreamReader.getLocalName();
          if ("resource-description".equals(str1))
          {
            j = 0;
          }
          else if ("weblogic-enterprise-bean".equals(str1))
          {
            WRITER.println(EAR_NAME + "," + REFNAME + "," + QUEUENAME + "," + EJBNAME + "," + APP_TARGET);
            QUEUENAME = null;
            EJBNAME = null;
          }
        }
      }
    }
    catch (XMLStreamException localXMLStreamException)
    {
      System.out.println(localXMLStreamException.getMessage());
      if (localXMLStreamException.getNestedException() != null) {
        localXMLStreamException.getNestedException().printStackTrace();
      }
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
    }
  }
  
  public static void processConfigFile(String paramString)
    throws Exception
  {
    try
    {
      XMLStreamReader localXMLStreamReader = xmlif.createXMLStreamReader(new FileInputStream(paramString));
      
      int j = 0;
      
      Object localObject = null;
      String str3 = null;
      while (localXMLStreamReader.hasNext())
      {
        int i = localXMLStreamReader.next();
        String str1;
        if (1 == i)
        {
          str1 = localXMLStreamReader.getLocalName();
          if (("name".equals(str1)) && (j != 0))
          {
            String str2 = localXMLStreamReader.getElementText();
            if (str2.startsWith("_ALSB_") || str2.startsWith("SB_")) {
              localObject = str2;
            }
          }
          else if ("target".equals(str1))
          {
            if (j != 0) {
              str3 = localXMLStreamReader.getElementText();
            }
          }
          else if ("app-deployment".equals(str1))
          {
            j = 1;
          }
        }
        else if (2 == i)
        {
          str1 = localXMLStreamReader.getLocalName();
          if ("app-deployment".equals(str1))
          {
            j = 0;
            if (localObject != null) {
              DEPLOYMENTS.put((String) localObject, str3);
            }
            localObject = null;
            str3 = null;
          }
        }
      }
    }
    catch (XMLStreamException localXMLStreamException)
    {
      System.out.println(localXMLStreamException.getMessage());
      if (localXMLStreamException.getNestedException() != null) {
        localXMLStreamException.getNestedException().printStackTrace();
      }
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
    }
  }
}
