/*
 *
 * IBM and Sterling Commerce Confidential 
 *
 * OCO Source Materials 
 *
 * IBM B2B Sterling Integrator 
 *
 * (c) Copyright Sterling Commerce, an IBM Company 2001, 2011
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office. 
 *
 */

package com.sterlingcommerce.woodstock.ui.jspbean;

import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.Comparator;
import java.util.Collections;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.sterlingcommerce.woodstock.adminui.jspbean.DigiCertBean;
import com.sterlingcommerce.woodstock.ebics.subscrmgr.HostProfileMgr;
import com.sterlingcommerce.woodstock.ebics.subscrmgr.ProfileMgrDBProxy;
import com.sterlingcommerce.woodstock.ebics.subscrmgr.OfferMgrDBProxy;
import com.sterlingcommerce.woodstock.ebics.subscrmgr.OrderTypeMgr;
import com.sterlingcommerce.woodstock.ebics.subscrmgr.OrderTypeMgr.OrdFormatMgr;
import com.sterlingcommerce.woodstock.ebics.subscrmgr.HostProfileMgr.HostProtocolMgr;
import com.sterlingcommerce.woodstock.ebics.subscrmgr.HostProfileMgr.HostUrlMgr;
import com.sterlingcommerce.woodstock.ebics.subscrmgr.HostProfileMgr.HostVerMgr;
import com.sterlingcommerce.woodstock.ebics.util.EBICSConstants;
import com.sterlingcommerce.woodstock.ebics.util.EBICSUtils;
import com.sterlingcommerce.woodstock.ebics.keymgmt.KeyUtil;
import com.sterlingcommerce.woodstock.ui.BankProfileEditor;
import com.sterlingcommerce.woodstock.ui.SessionData;
import com.sterlingcommerce.woodstock.ui.SessionDataContainer;
import com.sterlingcommerce.woodstock.ui.SessionInfo;
import com.sterlingcommerce.woodstock.ui.SystemCertLister;
import com.sterlingcommerce.woodstock.ui.TrustedCertLister;
import com.sterlingcommerce.woodstock.ui.UIGlobals;
import com.sterlingcommerce.woodstock.ui.UIPermissions;
import com.sterlingcommerce.woodstock.ui.UserAutho;
import com.sterlingcommerce.woodstock.ui.Wizard;
import com.sterlingcommerce.woodstock.ui.WizardObject;
import com.sterlingcommerce.woodstock.ui.WizardStep;
import com.sterlingcommerce.woodstock.ui.jspbean.EBICSBeanBase.Paginator;
import com.sterlingcommerce.woodstock.ui.servlet.WizardBase;
import com.sterlingcommerce.woodstock.ui.utils.html.HTMLButtonBuilder;
import com.sterlingcommerce.woodstock.util.NameValue;
import com.sterlingcommerce.woodstock.util.NameValuePairs;
import com.sterlingcommerce.woodstock.util.Util;
import com.sterlingcommerce.woodstock.util.frame.Manager;
import com.yantra.yfc.date.YTimestamp;
import com.yantra.yfc.core.YFCObject;

/**
 * Title: 			BankProfileBean.java
 * Description: 	The BankProfileBean class is a bean used in the Bank configuration Wizard to create new Bank Profile.
 *
 * @author 			Maheshwari Ramakrishnan.
 * @created 		12 June 2009
 * @version 		1.0
 *
 * Revision History	1.0 	- Initial Version
 *
 * QC#    Date            Fixed By     Description
 * 222262 30/5/2010		  Terence	   The delete wizard needs to include a request parameter i.e. returnDelete to indicate that the list
 *                                     page is being called after a delete operation. If there are no records available after a delete
 *                                     operation, then we will not display an error but simply return to the main search page.                                    
 * 199380 8/6/2010		  Terence	   The select boxes for the Country and Time Zone should be defaulted to a blank value. 
 */


public class BankProfileBean extends ResourceMgmtBean implements WizardBean {

	BankProfileEditor editor;

    private ArrayList resultList = null;
    private ArrayList bankProfileList = null;
    private String displayquery;
    private String bankID;

    private String alphaSelect;
    private String id;
    private String displayname;
    private boolean asc = true;
    private boolean hasDeletePerm = false;

    private static final String FLIKE = "FLIKE";
	private static final String LIKE = "LIKE";

	public final static String DATE_ENTRY_PATTERN = "(MM/DD/YYYY)";
	public final static String AVAILABLE = "Available";
	public final static String NOTAVAILABLE = " Not Available";

	public final static int ACTIVE = 1;
	public final static int INACTIVE = 0;

	private int totalCount; 	 //  Holds the number of all results.

	private int pos = 0;  		// Holds the position to start displaying results.

	private int num = 15; 		// Holds the number of results to display.

	private int stepSize = -1;	// Holds the stepsize for the jumpbar.

	private String[] keys ; 	//paging support

	public NameValuePairs sysCertList = (new SystemCertLister()).getOptionList("byid", null, session);
	public NameValuePairs trustedCertList = (new TrustedCertLister()).getOptionList("byid", null,session);
	
	public String getEncType() {
		return "multi-part/form-data";
	
	}
	
	public String getHeader() {

    	String name = null;
    	String header = null;
		editor = (BankProfileEditor)wiz.editObj();

		if ( editor != null ) {
			if(editor.getHostProfileMgr()!=null){
			name = ((HostProfileMgr)editor.getHostProfileMgr()).getHostID();
			}}
		if ( page.pageType.equals("CONFIRM")) {
			if ( name != null && name.length() > 0 && page.activity.equalsIgnoreCase("bankConfirmEdit")) {
				header =super.getHeader();
				return (header != null)? name + ":" + header: name;
			}
		}else {
			if ( name != null && name.length () > 0 ){
				header =super.getHeader();
				return (header != null)? name + ":" + header: name;
			}
		}
		return  super.getHeader ();
	}

	/**
     * Initializes the bean with the current state.
     * Make sure any instance variables are set appropriately.
     * @param wiz the Wizard object
     * @param page the WizardStep object for the current page.
     * @param pageno the page number
     * @param langbean The LangBean object held by the user so that
     * language specific pages can be built.
     */
    public void init(Wizard wiz, WizardStep page, String pageno,
                     LangBean langbean) {

        super.init(wiz, page, pageno, langbean);
        editor = (BankProfileEditor)wiz.editObj();
    }


    public void initInfo(HttpSession session, HttpServletRequest request) {
        setLang((LangBean)session.getAttribute("langbean"));
        String wizType = request.getParameter(WizardBase.WIZARD_TYPE);
        String objType = request.getParameter(WizardBase.WIZARD_OBJECT_TYPE);

        Wizard wiz = (Wizard)session.getAttribute(wizType +"_" + objType);
        if (wiz!=null) {
        	editor = (BankProfileEditor)wiz.editObj();
        }
    }


    /**
     * Builds the appropriate html inputs for the current page.
     * @return String
     */
    public String getInputs() {
        StringBuffer s = new StringBuffer();
        s.append("");

        if (page.pageType.equals("BANK_YFS_INFO")) {
        	buildYFSBankProfileInfo(s);
    	} else if (page.pageType.equals("BANK_INFO")) {
    		buildBankProfileInfo(s);
        } else if (page.pageType.equals("BANK_INFO_RSA")) {
    		buildBankProfileRSAInfo(s);
        } else if (page.pageType.equals("BANK_URL")) {
    		buildBankURL(s);
        } else if (page.pageType.equals("BANK_URL_INFO")) {
    		buildBankURLInfo(s);
        } else if (page.pageType.equals("BANK_PROTOCOL")) {
    		buildBankProtocol(s);
        } else if (page.pageType.equals("BANK_PROTOCOL_INFO")) {
    		buildBankProtocolInfo(s);
        } else if (page.pageType.equals("BANK_PROCESS")) {
    		buildBankProcess(s);
        } else if (page.pageType.equals("BANK_PROCESS_INFO")) {
    		buildBankProcessInfo(s);
        } else if (page.pageType.equals("CONFIRM")) {
            buildConfirm(s);
        }

        return s.toString();
    }

    /**
     * Generates the appropriate javascript validators for the inputs on
     * the page.
     */
    public String getValidator() {

    	StringBuffer s = new StringBuffer();
        s.append("");

        if( page.pageType.equals("BANK_YFS_INFO")){
        	s.append("if(activity!=\"Back\"){");
        	addFilledTest(s,"bankID",langbean.getValue("Label.BankID"),"1");
	        addFilledTest(s,"bankName",langbean.getValue("Label.BankName"),"1");
			//Bank ID validation
        	s.append("var element  = form.bankID.value + \"\";");
        	s.append("var isValid  = true;");
        	s.append("var validSpec = \"&$#@!^%*()<>?|:~{}[];'+,= \\\\\";");
        	s.append("for (var i=0; i<element.length; i++) { \n");
        	s.append("if(validSpec.indexOf(element.charAt(i)) != -1) {\n");
        	s.append("isValid = false;");
        	s.append("} \n } \n ");
        	s.append("if(!isValid) {\n");
        	s.append("  alert(\"" + Util.replaceString(langbean.getValue("error.invalidBankID"), "'", "\\'") + "\");");
        	s.append("  form.bankID.focus();");
        	s.append("    return false;");
        	s.append("} \n ");
	        addValidCharsNoSpaceTest(s, "bankID", langbean.getValue("Label.BankID"),"1");

	        addCheckMaxLength(s , "bankID", 35, langbean.getValue("Label.BankID") , "1");
		
	        //Bank Name --valid_no_slashes
        	s.append("var ele  = form.bankName.value + \"\";");
	        s.append("var isValid  = true;");
           	s.append("var inValidSpec = \"#@!^%*()<>?|~{}[];'+,=\\\\\";");
           	s.append("for (var i=0; i<ele.length; i++) { \n");
            s.append("if(inValidSpec.indexOf(ele.charAt(i)) != -1) {\n");
            s.append("isValid = false;");
            s.append("} \n } \n ");
            s.append("if(!isValid) {\n");
        	s.append("  alert(\"" + Util.replaceString(langbean.getValue("error.invalidBankName"), "'", "\\'") + "\");");
	        s.append("  form.bankName.focus();");
        	s.append("    return false;");
	        s.append("} \n ");
	
	        //Address1 validation
        	s.append("var ele  = form.addressLine1.value + \"\";");
        	s.append("var inValidSpec  = \"@!~%^*()+={}[]<>;:?|\";");
        	s.append("var isValid  = true;");
        	s.append("for (var i=0; i<ele.length; i++) { \n");
        	s.append("if(inValidSpec.indexOf(ele.charAt(i)) != -1) {\n");
        	s.append("isValid = false;");
        	s.append("} \n } \n ");
        	s.append("if(!isValid) {\n");
        	s.append("  alert(\"" + Util.replaceString(langbean.getValue("error.invalidAddress"), "'", "\\'") + "\");");
        	s.append("  form.addressLine1.focus();");
        	s.append("    return false;");
        	s.append("} \n ");
        	
        	//Address2 validation
        	s.append("var ele  = form.addressLine2.value + \"\";");
        	s.append("var inValidSpec  = \"@!~%^*()+={}[]<>;:?|\";");
        	s.append("var isValid  = true;");
        	s.append("for (var i=0; i<ele.length; i++) { \n");
        	s.append("if(inValidSpec.indexOf(ele.charAt(i)) != -1) {\n");
        	s.append("isValid = false;");
        	s.append("} \n } \n ");
        	s.append("if(!isValid) {\n");
        	s.append("  alert(\"" + Util.replaceString(langbean.getValue("error.invalidAddress"), "'", "\\'") + "\");");
        	s.append("  form.addressLine2.focus();");
        	s.append("    return false;");
        	s.append("} \n ");
        	
        	//City validation
        	s.append("var ele  = form.city.value + \"\";");
        	s.append("var invalidSpec  = \"@!~%^*()+={}[]<>;:?|1234567890\";");
        	s.append("var isValid  = true;");
        	s.append("for (var i=0; i<ele.length; i++) { \n");
        	s.append("if(invalidSpec.indexOf(ele.charAt(i)) != -1) {\n");
        	s.append("isValid = false;");
        	s.append("} \n } \n ");
        	s.append("if(!isValid) {\n");
        	s.append("  alert(\"" + Util.replaceString(langbean.getValue("error.invalidCity"), "'", "\\'") + "\");");
        	s.append("  form.city.focus();");
        	s.append("    return false;");
        	s.append("} \n ");
        	
        	//State validation
        	s.append("var ele  = form.state.value + \"\";");
        	s.append("var invalidSpec  = \"@!~%^*()+={}[]<>;:?|1234567890\";");
        	s.append("var isValid  = true;");
        	s.append("for (var i=0; i<ele.length; i++) { \n");
        	s.append("if(invalidSpec.indexOf(ele.charAt(i)) != -1) {\n");
        	s.append("isValid = false;");
        	s.append("} \n } \n ");
        	s.append("if(!isValid) {\n");
        	s.append("  alert(\"" + Util.replaceString(langbean.getValue("error.invalidState"), "'", "\\'") + "\");");
        	s.append("  form.state.focus();");
        	s.append("    return false;");
        	s.append("} \n ");
        	
        	//Zip validation
        	s.append("var ele  = form.zip.value + \"\";");
        	s.append("var isValid  = true;");
        	s.append("for (var i=0; i<ele.length; i++) { \n");
        	s.append("if(validNumAlpha.indexOf(ele.charAt(i)) == -1) {\n");
        	s.append("isValid = false;");
        	s.append("} \n } \n ");
        	s.append("if(!isValid) {\n");
        	s.append("  alert(\"" + Util.replaceString(langbean.getValue("error.invalidZip"), "'", "\\'") + "\");");
        	s.append("  form.zip.focus();");
        	s.append("    return false;");
        	s.append("} \n ");
        	
        	//phone validation
        	s.append("var ele  = form.dayphone.value + \"\";");
        	s.append("var validSpec  = \"-\";");
        	s.append("var isValid  = true;");
        	s.append("for (var i=0; i<ele.length; i++) { \n");
        	s.append("if(validNumbers.indexOf(ele.charAt(i)) == -1 && validSpec.indexOf(ele.charAt(i)) == -1) {\n");
        	s.append("isValid = false;");
        	s.append("} \n } \n ");
        	s.append("if(!isValid) {\n");
        	s.append("  alert(\"" + Util.replaceString(langbean.getValue("error.invalidPhoneNo"), "'", "\\'") + "\");");
        	s.append("  form.dayphone.focus();");
        	s.append("    return false;");
        	s.append("} \n ");

	        addEmailTest(s, "emailID", langbean.getValue("CP.EmailAddress"),"0");
	        s.append("}");
        } else if(page.pageType.equals("BANK_INFO")){
        	s.append("if(activity!=\"Back\"){");
        	addIntTest(s,"maxRecovery",langbean.getValue("Label.MAXRecovery"));
        	addIntTest(s,"maxSignAllowed",langbean.getValue("Label.MaxSignAllowed"));
        	s.append("} \n ");
        } else if ( page.pageType.equals("BANK_URL_INFO") ) {
        	s.append("if(activity==\"Next\"){");
            addFilledTest(s,"URLName",langbean.getValue("wizard.bankURL"),"1");
        	addValidCharsForURLTest(s,"URLName",langbean.getValue("wizard.bankURL"),"1");
            s.append("    if (form.WizardAction.value == \"Next\") {\n");
            s.append("        form.isNextOnURLinfo.value=\"Yes\";\n");
            s.append("        form.WizardAction.value=\"Back\";\n"); //always go back to the list
            s.append("    }\n");
            s.append("}");
        }else if ( page.pageType.equals("BANK_PROCESS") ) {
        	s.append("if(activity==\"Next\"){");
            s.append("    if (form.WizardAction.value == \"Next\") {\n");
            s.append("        form.isActionNext.value=\"Yes\";\n");
            s.append("        form.WizardAction.value=\"Back\";\n"); //always go back to the list page.
            s.append("    }\n");
            s.append("}");
        }else if ( page.pageType.equals("BANK_PROCESS_INFO") ) {
        	s.append("if(activity==\"Next\"){");
        	addFilledTest(s,"processType",langbean.getValue("wizard.processType"),"1");
        	addFilledTest(s,"processVer",langbean.getValue("wizard.processVersion"),"1");
        	s.append("}");
        	s.append("if(activity==\"Next\"){");
            s.append("    if (form.WizardAction.value == \"Next\") {\n");
            s.append("        form.isNextOnProcessInfo.value=\"Yes\";\n");
            s.append("        form.WizardAction.value=\"Back\";\n"); //always go back to the list
            s.append("    }\n");
            s.append("}");
        }else if ( page.pageType.equals("BANK_PROTOCOL_INFO") ) {
        	s.append("if(activity==\"Next\"){");
        	addFilledTest(s,"protocolVersion",langbean.getValue("wizard.protocolVer"),"1");
        	 s.append("       form.isNextOnProtocolInfo.value=\"Yes\";\n");
        	addFilledTest(s,"releaseVersion",langbean.getValue("wizard.releaseVer"),"1");
        	s.append("}");
        }
        return s.toString();
    }


    /**
	 * Retrieves a display formatted query.
	 *
	 * @return String
	 */
	public String getDisplayQuery() {
		return displayquery;
	}
	
	 private void buildBankCertOption(StringBuffer buf) { 
		 	wiz.linksActive = false;
		        String val = "";
		        String className = "whiteStripe";
		        buf.append("<table>\n<tr>\n");
		        buf.append("</tr>\n");

		        buf.append("<tr>");
		        buf.append("<td >");
		        buildImgRadio(buf, 0, "bankCertOption", "Keys", "Keys", val, className);
		        buf.append("</td>");
		        buf.append("</tr>");

		        buf.append("<tr>");
		        buf.append("<td >");
		        buildImgRadio(buf, 1, "bankCertOption", "X509", "X509", val, className);
		        //buf.append("<input name='systemCertOption' id='systemCertOption' value='yes' type='hidden'>\n");
		        buf.append("</td>");
		        buf.append("</tr>");
		 	 
		        imageRadioJavascript(buf, "bankCertOption", val, 2); 	 
		        buf.append("</table>\n"); 	 
		    }


    private void buildYFSBankProfileInfo(StringBuffer htmlBuf) {

    	htmlBuf.append("<table>\n");
        String val = editor.getHostProfileMgr().getHostID();
        if( val == null ) {
            val = "";
        }
        if(editor.getActionType()==1){
			htmlBuf.append("<tr>\n<td></td>\n");
			 buildInputBox(htmlBuf,langbean.getValue("Label.BankID"),  "bankID", val, "45", "35",
                     "<td valign=\"bottom\" class='WizardDeploymentText' nowrap>",
                     "</td><td valign=\"bottom\" class=\"WizardInputData\">",
                     "</td>");

	        htmlBuf.append ("</tr>\n");
        }
        else {
        	htmlBuf.append("<tr><td></td>\n");
        	htmlBuf.append ("<td valign='bottom' class='WizardDeploymentText'>\n");
        	htmlBuf.append (langbean.getValue ("Label.BankID"));
        	htmlBuf.append (": </td>\n");
        	htmlBuf.append("<td valign='bottom' class='WizardInputData'>");
        	htmlBuf.append("<input type='hidden' name='bankID'");
        	htmlBuf.append(" value='");
        	htmlBuf.append(UIGlobals.processHTMLDisplayValue(val));
        	htmlBuf.append("'>");
        	htmlBuf.append(UIGlobals.processHTMLDisplayValue(val));
        	htmlBuf.append ("</td>\n");
        	htmlBuf.append("</tr>\n");
		}

        val=editor.getHostProfileMgr().getFirstName();
        if(val==null){
        	val="";
        }

        htmlBuf.append("<tr>\n<td></td>\n");
        buildInputBox(htmlBuf,langbean.getValue("Label.BankName"),  "bankName", val, "45", "64",
                "<td valign=\"bottom\" class='WizardDeploymentText' nowrap>",
                "</td><td valign=\"bottom\" class=\"WizardInputData\">",
                "</td>");
        htmlBuf.append ("</tr>\n");
        
        val=editor.getHostProfileMgr().getAddressLine1();
        if(val==null){
        	val="";
        }

        htmlBuf.append("<tr>\n<td></td>\n");
        
        buildInputBox(htmlBuf, langbean.getValue("CP.Address1"), "addressLine1", val , "35", "70");
        htmlBuf.append ("</tr>\n");

        val=editor.getHostProfileMgr().getAddressLine2();
        if(val==null){
        	val="";
        }

        htmlBuf.append("<tr>\n<td></td>\n");
        buildInputBox(htmlBuf, langbean.getValue("CP.Address2"), "addressLine2", val , "35", "70");
        htmlBuf.append ("</tr>\n");

        val=editor.getHostProfileMgr().getCity();
        if(val==null){
        	val="";
        }

        htmlBuf.append("<tr>\n<td></td>\n");
        buildInputBox(htmlBuf, langbean.getValue("CP.City"), "city", val , "35", "35");
        htmlBuf.append ("</tr>\n");

        val=editor.getHostProfileMgr().getState();
        if(val==null){
        	val="";
        }

        htmlBuf.append("<tr>\n<td></td>\n");
        buildInputBox(htmlBuf, langbean.getValue("CP.State"), "state", val , "35", "35");
        htmlBuf.append ("</tr>\n");

        val = editor.getHostProfileMgr().getCountry();
        if(val == null){
            val = "";
        }
        htmlBuf.append("<tr>\n<td></td>\n");
        htmlBuf.append("<td valign=\"bottom\" class=\"WizardInputText\">");
        htmlBuf.append(langbean.getValue("CP.Country"));
        htmlBuf.append(":</td><td>");
        //QC#199380
	    buildSelectList(htmlBuf, "country", DigiCertBean.getCountries(langbean), val, true, 0, 1);
	    htmlBuf.append("</td>\n");
	    htmlBuf.append("</tr>\n");

        val=editor.getHostProfileMgr().getZipCode();
        if(val==null){
        	val="";
        }

        htmlBuf.append("<tr>\n<td></td>\n");
        buildInputBox(htmlBuf, langbean.getValue("CP.Zip"), "zip", val , "35", "35");
        htmlBuf.append ("</tr>\n");

        val=editor.getHostProfileMgr().getTimeZone();
        if(val==null){
        	val="";
        }

        htmlBuf.append("<tr>\n<td></td>\n");
		 /* Remove the empty option added by QC#199380,
		  * this is to fix the deletion error when deleting the EBICS partner 
		  * from Trading Partner/Setup/Advanced/Identities UI, if no timezone is set to EBICS partner. 
		  * Apply this default timezone to EBICS Bank/Partner/User.
		  */ 
		 //String timeList = "<option value=\"\">" + langbean.getValue("timeSelect");
		 String timeList = langbean.getValue("timeSelect");
		 if(val != null && val.length() > 0){
            timeList = Util.replaceString(timeList, "value=\"" +val+ "\"",
                    "value=\""+val+"\" SELECTED ");}
        htmlBuf.append("<td valign=\"bottom\" class=\"WizardInputText\">");
        htmlBuf.append(langbean.getValue("CP.TimeZone"));
        htmlBuf.append(":</td><td valign=\"bottom\" width=1 class='WizardInputText'><SELECT name=\"timeZone\">");
        htmlBuf.append(timeList);
        htmlBuf.append("</SELECT></td>");

        htmlBuf.append("</tr><tr>\n");

        //QC#199380 - Commented the script to set the default time zone value        
        //javascript to get client time zone and set as default in select list
        /* if(val == null || val.equals("")){
        	htmlBuf.append("<SCRIPT LANGUAGE='JavaScript1.2'>");
        	htmlBuf.append("var now = new Date();");
        	htmlBuf.append("var timezoneoffset = ((now.getTimezoneOffset()/60)+1)*-1;");
        	htmlBuf.append("list = eval(\"document.wizform.timeZone\");");
        	htmlBuf.append("for(var i=0; i < list.options.length; i++){");
        	htmlBuf.append("if(list.options[i].value == timezoneoffset) list.options[i].selected = true;}");
        	htmlBuf.append("</script>");
        } */

        val=editor.getHostProfileMgr().getEmailID();
        if(val==null){
        	val="";
        }

        htmlBuf.append("<tr>\n<td></td>\n");
        buildInputBox(htmlBuf, langbean.getValue("CP.EmailAddress"), "emailID", val , "35", "150");
        htmlBuf.append ("</tr>\n");

        val=editor.getHostProfileMgr().getDayPhone();
        if(val==null){
        	val="";
        }

        htmlBuf.append("<tr>\n<td></td>\n");
        buildInputBox(htmlBuf, langbean.getValue("CP.PhoneNumber"), "dayphone", val , "35", "40");
        htmlBuf.append ("</tr>\n");

        htmlBuf.append ("</table>\n");
        buildBankCertOption(htmlBuf);
    }


    private void buildBankProfileInfo(StringBuffer htmlBuf) {

    	int status=-1;
    	String val =null;
    	htmlBuf.append("<table>\n");


    	if(editor.getHostProfileMgr()!=null) {
    		val= sysCertList.getName(editor.getHostProfileMgr().getEncrPrivKeyID());
    	}
    	if(val==null){
    		val="";
    	}
    	htmlBuf.append("<tr>\n");

    	htmlBuf.append("<td valign=\"middle\" nowrap class='WizardInputText'>");
    	htmlBuf.append(langbean.getValue("Label.EncPriCert"));
    	htmlBuf.append(":");
    	htmlBuf.append("</td>");
    	htmlBuf.append("<td nowrap valign=\"middle\" class=\"WizardInputText\">");
    	htmlBuf.append("<input readonly=\"true\" type=text name=\"EncPriCert\"  value=\"");
    	htmlBuf.append(val);
    	htmlBuf.append("\" size=30 maxlength=250>");
    	htmlBuf.append("&nbsp;");

    	htmlBuf.append("<td>\n");
    	htmlBuf.append("<a class='selectResource' href=\"javascript:openSizableSubProcessWin('./Page?");
    	htmlBuf.append(uniqueKey());
    	htmlBuf.append(attachWizInfoToUrl("&next=page.sysCertPicker&view=list&undo=false&pos=0&num=15"));
    	htmlBuf.append("&certField=");
    	htmlBuf.append(editor.EncPriCert);
    	htmlBuf.append("','picker','550','500')\"");
    	htmlBuf.append(" onmouseover=\"window.status='");
    	htmlBuf.append(langbean.getValue("SelectFromListView"));
    	htmlBuf.append("';return true;\" onmouseout=\"window.status=''; return true;\" >");
        makeImage(htmlBuf, "./images/blue/search.gif", null, null, "ListView", " align='absbottom' alt='"+langbean.getValue("ListView").replace("'","&#146")+"'");
        htmlBuf.append("</a>\n");
        htmlBuf.append("</td>\n");

    	if(editor.getHostProfileMgr()!=null) {
    		val= trustedCertList.getName(editor.getHostProfileMgr().getEncrPubKeyID());
    	}
    	if(val==null){
    		val="";
    	}

    	htmlBuf.append("<tr>\n");

    	htmlBuf.append("<td valign=\"middle\" nowrap class='WizardInputText'>");
    	htmlBuf.append(langbean.getValue("Label.EncPubCert"));
    	htmlBuf.append(":");
    	htmlBuf.append("</td>");
    	htmlBuf.append("<td nowrap valign=\"middle\" class=\"WizardInputText\">");
    	htmlBuf.append("<input readonly=\"true\" type=text name=\"EncPubCert\"  value=\"");
    	htmlBuf.append(val);
    	htmlBuf.append("\" size=30 maxlength=250>");
    	htmlBuf.append("&nbsp;");


    	htmlBuf.append("<td>\n");
    	htmlBuf.append("<a class='selectResource' href=\"javascript:openSizableSubProcessWin('./Page?");
    	htmlBuf.append(uniqueKey());
    	htmlBuf.append(attachWizInfoToUrl("&next=page.sysCertPicker&view=list&undo=false&pos=0&num=15"));
    	htmlBuf.append("&certField=");
    	htmlBuf.append(editor.EncPubCert);
    	htmlBuf.append("','picker','550','500')\"");
    	htmlBuf.append(" onmouseover=\"window.status='");
    	htmlBuf.append(langbean.getValue("SelectFromListView"));
    	htmlBuf.append("';return true;\" onmouseout=\"window.status=''; return true;\" >");
        makeImage(htmlBuf, "./images/blue/search.gif", null, null, "ListView", " align='absbottom' alt='"+langbean.getValue("ListView").replace("'","&#146")+"'");
        htmlBuf.append("</a>\n");
        htmlBuf.append("</td>\n");

    	if(editor.getHostProfileMgr()!=null) {
    		val=sysCertList.getName(editor.getHostProfileMgr().getAuthPrivKeyID());
    	}
    	if(val==null){
    		val="";
    	}


    	htmlBuf.append("<tr>\n");

    	htmlBuf.append("<td valign=\"middle\" nowrap class='WizardInputText'>");
    	htmlBuf.append(langbean.getValue("Label.AuthPriCert"));
    	htmlBuf.append(":");
    	htmlBuf.append("</td>");
    	htmlBuf.append("<td nowrap valign=\"middle\" class=\"WizardInputText\">");
    	htmlBuf.append("<input readonly=\"true\" type=text name=\"AuthPriCert\"  value=\"");
    	htmlBuf.append(val);
    	htmlBuf.append("\" size=30 maxlength=250>");
    	htmlBuf.append("&nbsp;");

    	htmlBuf.append("<td>\n");
    	htmlBuf.append("<a class='selectResource' href=\"javascript:openSizableSubProcessWin('./Page?");
    	htmlBuf.append(uniqueKey());
    	htmlBuf.append(attachWizInfoToUrl("&next=page.sysCertPicker&view=list&undo=false&pos=0&num=15"));
    	htmlBuf.append("&certField=");
    	htmlBuf.append(editor.AuthPriCert);
    	htmlBuf.append("','picker','550','500')\"");
    	htmlBuf.append(" onmouseover=\"window.status='");
    	htmlBuf.append(langbean.getValue("SelectFromListView"));
    	htmlBuf.append("';return true;\" onmouseout=\"window.status=''; return true;\" >");
        makeImage(htmlBuf, "./images/blue/search.gif", null, null, "ListView", " align='absbottom' alt='"+langbean.getValue("ListView").replace("'","&#146")+"'");
        htmlBuf.append("</a>\n");
        htmlBuf.append("</td>\n");

    	if(editor.getHostProfileMgr()!=null) {
    		val=trustedCertList.getName(editor.getHostProfileMgr().getAuthPubKeyID());
    	}
    	if(val==null){
    		val="";
    	}


    	htmlBuf.append("<tr>\n");

    	htmlBuf.append("<td valign=\"middle\" nowrap class='WizardInputText'>");
    	htmlBuf.append(langbean.getValue("Label.AuthPubCert"));
    	htmlBuf.append(":");
    	htmlBuf.append("</td>");
    	htmlBuf.append("<td nowrap valign=\"middle\" class=\"WizardInputText\">");
    	htmlBuf.append("<input readonly=\"true\" type=text name=\"AuthPubCert\"  value=\"");
    	htmlBuf.append(val);
    	htmlBuf.append("\" size=30 maxlength=250>");
    	htmlBuf.append("&nbsp;");

    	htmlBuf.append("<td>\n");
    	htmlBuf.append("<a class='selectResource' href=\"javascript:openSizableSubProcessWin('./Page?");
    	htmlBuf.append(uniqueKey());
    	htmlBuf.append(attachWizInfoToUrl("&next=page.sysCertPicker&view=list&undo=false&pos=0&num=15"));
    	htmlBuf.append("&certField=");
    	htmlBuf.append(editor.AuthPubCert);
    	htmlBuf.append("','picker','550','500')\"");
    	htmlBuf.append(" onmouseover=\"window.status='");
    	htmlBuf.append(langbean.getValue("SelectFromListView"));
    	htmlBuf.append("';return true;\" onmouseout=\"window.status=''; return true;\" >");
        makeImage(htmlBuf, "./images/blue/search.gif", null, null, "ListView", " align='absbottom' alt='"+langbean.getValue("ListView").replace("'","&#146")+"'");
        htmlBuf.append("</a>\n");
        htmlBuf.append("</td>\n");

    	val= Long.toString(editor.getHostProfileMgr().getMaxRecoveryNum());
    	if(val==null){
    		val="";
    	}
    	// One Line space.
    	htmlBuf.append("<tr>\n<td></td>\n");
    	htmlBuf.append ("</tr>\n");

    	htmlBuf.append("<tr>\n");
    	buildInputBox(htmlBuf, langbean.getValue("Label.MAXRecovery"), "maxRecovery", val , "30", "10",
    			"<td valign=\"middle\" class=\"WizardDeploymentText\" nowrap>",
                "</td><td valign=\"middle\" class=\"WizardInputData\">","</td>");
    	htmlBuf.append ("</tr>\n");

    	val= editor.getHostProfileMgr().getPrevalidate();
    	if(val==null || val.equals("")){
    		status = ACTIVE;
    	}else {

    		if(val.trim().equals(EBICSConstants.DEF_SHORT_TRUE)) {
    			status = ACTIVE;
    		}else if (val.trim().equals(EBICSConstants.DEF_SHORT_FALSE)){
    			status = INACTIVE;
    		}
    	}
    	
    	if(editor.getHostProfileMgr()!=null) {
    		val=Long.toString(editor.getHostProfileMgr().getMaxSigAllowed());
    	}
    	if(val==null){
    		val="";
    	}

    	htmlBuf.append("<tr>");
    	buildInputBox(htmlBuf, langbean.getValue("Label.MaxSignAllowed"), "maxSignAllowed", val , "2", "2","<td valign=\"bottom\" class=\"WizardDeploymentText\" nowrap>",
                "</td><td valign=\"bottom\" class=\"WizardInputData\">","</td>");
        htmlBuf.append ("</tr>\n");
   	
    	htmlBuf.append("<tr><td class='whiteStripe' colspan=2>\n");
    	buildOnOffRadio(htmlBuf, "preValidation", langbean.getValue("Label.Prevalidation"), String.valueOf(1),
    			String.valueOf(status), "WizardInputRadio", "images.Deployment.onbutton");

    	if (status != -1) {
    		imageOnOffRadioJavascript(htmlBuf, "preValidation", String.valueOf(status),
    				String.valueOf(ACTIVE), String.valueOf(INACTIVE), "images.Deployment.onbutton");
    	}
    	else {
    		imageOnOffRadioJavascript(htmlBuf, "reset", "1", "1", "0", "images.Deployment.onbutton");
    	}

    	htmlBuf.append("</td></tr>\n");

    	val =editor.getHostProfileMgr().getClntDLSupport();
    	if(val==null || val.equals("")){
    		status=INACTIVE;
    	} else{
    		if(val.trim().equals(EBICSConstants.DEF_SHORT_TRUE)) {
    			status = ACTIVE;
    		} else if(val.trim().equals(EBICSConstants.DEF_SHORT_FALSE)){
    			status = INACTIVE;
    		}
    	}

    	htmlBuf.append("<tr><td class='whiteStripe' colspan=2>\n");
    	buildOnOffRadio(htmlBuf, "supportClientDownload", langbean.getValue("Label.SupportClientDownload"), String.valueOf(1),

    			String.valueOf(status), "WizardInputRadio", "images.Deployment.onbutton");

    	if (status != -1) {
    		imageOnOffRadioJavascript(htmlBuf, "supportClientDownload", String.valueOf(status),
    				String.valueOf(ACTIVE), String.valueOf(INACTIVE), "images.Deployment.onbutton");
    	}
    	else {
    		imageOnOffRadioJavascript(htmlBuf, "reset", "1", "1", "0", "images.Deployment.onbutton");
    	}

    	htmlBuf.append("</td></tr>\n");

    	val= editor.getHostProfileMgr().getDlOrdSupport();
    	if(val==null || val.equals("")){
    		status=INACTIVE;
    	} else{
    		if(val.trim().equals(EBICSConstants.DEF_SHORT_TRUE)) {
    			status = ACTIVE;
    		} else if(val.trim().equals(EBICSConstants.DEF_SHORT_FALSE)){
    			status = INACTIVE;
    		}
    	}

    	htmlBuf.append("<tr><td class='whiteStripe' colspan=2>\n");
    	buildOnOffRadio(htmlBuf, "supportOrderDownload", langbean.getValue("Label.SupportOrderDownload"), String.valueOf(1),
    			String.valueOf(status), "WizardInputRadio", "images.Deployment.onbutton");

    	if (status != -1) {
    		imageOnOffRadioJavascript(htmlBuf, "supportOrderDownload", String.valueOf(status),
    				String.valueOf(ACTIVE), String.valueOf(INACTIVE), "images.Deployment.onbutton");
    	}
    	else {
    		imageOnOffRadioJavascript(htmlBuf, "reset", "1", "1", "0", "images.Deployment.onbutton");
    	}

    	htmlBuf.append("</td></tr>\n");

    	val= editor.getHostProfileMgr().getX509Support();
    	if(val==null || val.equals("")){
    		status=ACTIVE;
    	}else{
    		if(val.trim().equals(EBICSConstants.DEF_SHORT_TRUE)) {
    			status = ACTIVE;
    		} else if(val.trim().equals(EBICSConstants.DEF_SHORT_FALSE)){
    			status = INACTIVE;
    		}
    	}

    	htmlBuf.append("<tr><td class='whiteStripe' colspan=2>\n");
    	buildOnOffRadio(htmlBuf, "supportX509", langbean.getValue("Label.SupportX509"), String.valueOf(1),
    			String.valueOf(status), "WizardInputRadio", "images.Deployment.onbutton");

    	if (status != -1) {
    		imageOnOffRadioJavascript(htmlBuf, "supportX509", String.valueOf(status),
    				String.valueOf(ACTIVE), String.valueOf(INACTIVE), "images.Deployment.onbutton");
    	}
    	else {
    		imageOnOffRadioJavascript(htmlBuf, "reset", "1", "1", "0", "images.Deployment.onbutton");
    	}

    	htmlBuf.append("</td></tr>\n");


    	val= editor.getHostProfileMgr().getX509Persist();
    	if(val==null || val.equals("")){
    		status=ACTIVE;
    	}else{
    		if(val.trim().equals(EBICSConstants.DEF_SHORT_TRUE)) {
    			status = ACTIVE;
    		} else if(val.trim().equals(EBICSConstants.DEF_SHORT_FALSE)){
    			status = INACTIVE;
    		}
    	}

    	htmlBuf.append("<tr><td class='whiteStripe' colspan=2>\n");
    	buildOnOffRadio(htmlBuf, "persistX509", langbean.getValue("Label.PersistX509"), String.valueOf(1),
    			String.valueOf(status), "WizardInputRadio", "images.Deployment.onbutton");

    	if (status != -1) {
    		imageOnOffRadioJavascript(htmlBuf, "persistX509", String.valueOf(status),
    				String.valueOf(ACTIVE), String.valueOf(INACTIVE), "images.Deployment.onbutton");
    	}
    	else {
    		imageOnOffRadioJavascript(htmlBuf, "reset", "1", "1", "0", "images.Deployment.onbutton");
    	}

    	htmlBuf.append("</td></tr>\n");
    	htmlBuf.append ("</table>\n");
    	}

    private void buildBankProfileRSAInfo(StringBuffer htmlBuf) {

    	int status=-1;
    	String val =null;
    	htmlBuf.append("<table>\n");


    	if(editor.getHostProfileMgr()!=null) {
    		val= sysCertList.getName(editor.getHostProfileMgr().getEncrPrivKeyID());
    	}
    	if(val==null){
    		val="";
    	}
    	htmlBuf.append("<tr>\n");

    	htmlBuf.append("<td valign=\"middle\" nowrap class='WizardInputText'>");
    	htmlBuf.append(langbean.getValue("Label.EncPriCert"));
    	htmlBuf.append(":");
    	htmlBuf.append("</td>");
    	htmlBuf.append("<td>");
    	
    	htmlBuf.append("<input type='file' name='EncPriCert' size=40>");
    	htmlBuf.append("</td>\n");
//    	htmlBuf.append("<td nowrap valign=\"middle\" class=\"WizardInputText\">");
//    	htmlBuf.append("<input readonly=\"true\" type=text name=\"EncPriCert\"  value=\"");
//    	htmlBuf.append(val);
//    	htmlBuf.append("\" size=30 maxlength=250>");
//    	htmlBuf.append("&nbsp;");
//
//    	htmlBuf.append("<td>\n");
//    	htmlBuf.append("<a class='selectResource' href=\"javascript:openSizableSubProcessWin('./Page?");
//    	htmlBuf.append(uniqueKey());
//    	htmlBuf.append(attachWizInfoToUrl("&next=page.sysCertPicker&view=list&undo=false&pos=0&num=15"));
//    	htmlBuf.append("&certField=");
//    	htmlBuf.append(editor.EncPriCert);
//    	htmlBuf.append("','picker','550','500')\"");
//    	htmlBuf.append(" onmouseover=\"window.status='");
//    	htmlBuf.append(langbean.getValue("SelectFromListView"));
//    	htmlBuf.append("';return true;\" onmouseout=\"window.status=''; return true;\" >");
//        makeImage(htmlBuf, "./images/blue/search.gif", null, null, "ListView", " align='absbottom' alt='"+langbean.getValue("ListView").replace("'","&#146")+"'");
//        htmlBuf.append("</a>\n");
//        htmlBuf.append("</td>\n");

    	if(editor.getHostProfileMgr()!=null) {
    		val= trustedCertList.getName(editor.getHostProfileMgr().getEncrPubKeyID());
    	}
    	if(val==null){
    		val="";
    	}

    	htmlBuf.append("<tr>\n");

    	htmlBuf.append("<td valign=\"middle\" nowrap class='WizardInputText'>");
    	htmlBuf.append(langbean.getValue("Label.EncPubCert"));
    	htmlBuf.append(":");
    	htmlBuf.append("</td>");
//    	htmlBuf.append("<td nowrap valign=\"middle\" class=\"WizardInputText\">");
//    	htmlBuf.append("<input readonly=\"true\" type=text name=\"EncPubCert\"  value=\"");
//    	htmlBuf.append(val);
//    	htmlBuf.append("\" size=30 maxlength=250>");
//    	htmlBuf.append("&nbsp;");


  //  	htmlBuf.append("<td>\n");
//    	htmlBuf.append("<a class='selectResource' href=\"javascript:openSizableSubProcessWin('./Page?");
//    	htmlBuf.append(uniqueKey());
//    	htmlBuf.append(attachWizInfoToUrl("&next=page.sysCertPicker&view=list&undo=false&pos=0&num=15"));
//    	htmlBuf.append("&certField=");
//    	htmlBuf.append(editor.EncPubCert);
//    	htmlBuf.append("','picker','550','500')\"");
//    	htmlBuf.append(" onmouseover=\"window.status='");
//    	htmlBuf.append(langbean.getValue("SelectFromListView"));
//    	htmlBuf.append("';return true;\" onmouseout=\"window.status=''; return true;\" >");
//        makeImage(htmlBuf, "./images/blue/search.gif", null, null, "ListView", " align='absbottom' alt='"+langbean.getValue("ListView").replace("'","&#146")+"'");
//        htmlBuf.append("</a>\n");
    	htmlBuf.append("<td>");
    	
    	htmlBuf.append("<input type='file' name='EncPubCert' size=40>");
    	htmlBuf.append("</td>\n");
     //   htmlBuf.append("</td>\n");

    	if(editor.getHostProfileMgr()!=null) {
    		val=sysCertList.getName(editor.getHostProfileMgr().getAuthPrivKeyID());
    	}
    	if(val==null){
    		val="";
    	}


    	htmlBuf.append("<tr>\n");

    	htmlBuf.append("<td valign=\"middle\" nowrap class='WizardInputText'>");
    	htmlBuf.append(langbean.getValue("Label.AuthPriCert"));
    	htmlBuf.append(":");
    	htmlBuf.append("</td>");
//    	htmlBuf.append("<td nowrap valign=\"middle\" class=\"WizardInputText\">");
//    	htmlBuf.append("<input readonly=\"true\" type=text name=\"AuthPriCert\"  value=\"");
//    	htmlBuf.append(val);
//    	htmlBuf.append("\" size=30 maxlength=250>");
//    	htmlBuf.append("&nbsp;");

//    	htmlBuf.append("<td>\n");
//    	htmlBuf.append("<a class='selectResource' href=\"javascript:openSizableSubProcessWin('./Page?");
//    	htmlBuf.append(uniqueKey());
//    	htmlBuf.append(attachWizInfoToUrl("&next=page.sysCertPicker&view=list&undo=false&pos=0&num=15"));
//    	htmlBuf.append("&certField=");
//    	htmlBuf.append(editor.AuthPriCert);
//    	htmlBuf.append("','picker','550','500')\"");
//    	htmlBuf.append(" onmouseover=\"window.status='");
//    	htmlBuf.append(langbean.getValue("SelectFromListView"));
//    	htmlBuf.append("';return true;\" onmouseout=\"window.status=''; return true;\" >");
//        makeImage(htmlBuf, "./images/blue/search.gif", null, null, "ListView", " align='absbottom' alt='"+langbean.getValue("ListView").replace("'","&#146")+"'");
//        htmlBuf.append("</a>\n");
//        htmlBuf.append("</td>\n");
    	htmlBuf.append("<td>");
    	
    	htmlBuf.append("<input type='file' name='AuthPriCert' size=40>");
    	htmlBuf.append("</td>\n");
    	if(editor.getHostProfileMgr()!=null) {
    		val=trustedCertList.getName(editor.getHostProfileMgr().getAuthPubKeyID());
    	}
    	if(val==null){
    		val="";
    	}


    	htmlBuf.append("<tr>\n");

    	htmlBuf.append("<td valign=\"middle\" nowrap class='WizardInputText'>");
    	htmlBuf.append(langbean.getValue("Label.AuthPubCert"));
    	htmlBuf.append(":");
    	htmlBuf.append("</td>");
//    	htmlBuf.append("<td nowrap valign=\"middle\" class=\"WizardInputText\">");
//    	htmlBuf.append("<input readonly=\"true\" type=text name=\"AuthPubCert\"  value=\"");
//    	htmlBuf.append(val);
//    	htmlBuf.append("\" size=30 maxlength=250>");
//    	htmlBuf.append("&nbsp;");
//
//    	htmlBuf.append("<td>\n");
//    	htmlBuf.append("<a class='selectResource' href=\"javascript:openSizableSubProcessWin('./Page?");
//    	htmlBuf.append(uniqueKey());
//    	htmlBuf.append(attachWizInfoToUrl("&next=page.sysCertPicker&view=list&undo=false&pos=0&num=15"));
//    	htmlBuf.append("&certField=");
//    	htmlBuf.append(editor.AuthPubCert);
//    	htmlBuf.append("','picker','550','500')\"");
//    	htmlBuf.append(" onmouseover=\"window.status='");
//    	htmlBuf.append(langbean.getValue("SelectFromListView"));
//    	htmlBuf.append("';return true;\" onmouseout=\"window.status=''; return true;\" >");
//        makeImage(htmlBuf, "./images/blue/search.gif", null, null, "ListView", " align='absbottom' alt='"+langbean.getValue("ListView").replace("'","&#146")+"'");
//        htmlBuf.append("</a>\n");
//        htmlBuf.append("</td>\n");
    	htmlBuf.append("<td>");
    	
    	htmlBuf.append("<input type='file' name='AuthPubCert' size=40>");
    	htmlBuf.append("</td>\n");

    	val= Long.toString(editor.getHostProfileMgr().getMaxRecoveryNum());
    	if(val==null){
    		val="";
    	}
    	// One Line space.
    	htmlBuf.append("<tr>\n<td></td>\n");
    	htmlBuf.append ("</tr>\n");

    	htmlBuf.append("<tr>\n");
    	buildInputBox(htmlBuf, langbean.getValue("Label.MAXRecovery"), "maxRecovery", val , "30", "10",
    			"<td valign=\"middle\" class=\"WizardDeploymentText\" nowrap>",
                "</td><td valign=\"middle\" class=\"WizardInputData\">","</td>");
    	htmlBuf.append ("</tr>\n");

    	val= editor.getHostProfileMgr().getPrevalidate();
    	if(val==null || val.equals("")){
    		status = ACTIVE;
    	}else {

    		if(val.trim().equals(EBICSConstants.DEF_SHORT_TRUE)) {
    			status = ACTIVE;
    		}else if (val.trim().equals(EBICSConstants.DEF_SHORT_FALSE)){
    			status = INACTIVE;
    		}
    	}
    	
    	if(editor.getHostProfileMgr()!=null) {
    		val=Long.toString(editor.getHostProfileMgr().getMaxSigAllowed());
    	}
    	if(val==null){
    		val="";
    	}

    	htmlBuf.append("<tr>");
    	buildInputBox(htmlBuf, langbean.getValue("Label.MaxSignAllowed"), "maxSignAllowed", val , "2", "2","<td valign=\"bottom\" class=\"WizardDeploymentText\" nowrap>",
                "</td><td valign=\"bottom\" class=\"WizardInputData\">","</td>");
        htmlBuf.append ("</tr>\n");
   	
    	htmlBuf.append("<tr><td class='whiteStripe' colspan=2>\n");
    	buildOnOffRadio(htmlBuf, "preValidation", langbean.getValue("Label.Prevalidation"), String.valueOf(1),
    			String.valueOf(status), "WizardInputRadio", "images.Deployment.onbutton");

    	if (status != -1) {
    		imageOnOffRadioJavascript(htmlBuf, "preValidation", String.valueOf(status),
    				String.valueOf(ACTIVE), String.valueOf(INACTIVE), "images.Deployment.onbutton");
    	}
    	else {
    		imageOnOffRadioJavascript(htmlBuf, "reset", "1", "1", "0", "images.Deployment.onbutton");
    	}

    	htmlBuf.append("</td></tr>\n");

    	val =editor.getHostProfileMgr().getClntDLSupport();
    	if(val==null || val.equals("")){
    		status=INACTIVE;
    	} else{
    		if(val.trim().equals(EBICSConstants.DEF_SHORT_TRUE)) {
    			status = ACTIVE;
    		} else if(val.trim().equals(EBICSConstants.DEF_SHORT_FALSE)){
    			status = INACTIVE;
    		}
    	}

    	htmlBuf.append("<tr><td class='whiteStripe' colspan=2>\n");
    	buildOnOffRadio(htmlBuf, "supportClientDownload", langbean.getValue("Label.SupportClientDownload"), String.valueOf(1),

    			String.valueOf(status), "WizardInputRadio", "images.Deployment.onbutton");

    	if (status != -1) {
    		imageOnOffRadioJavascript(htmlBuf, "supportClientDownload", String.valueOf(status),
    				String.valueOf(ACTIVE), String.valueOf(INACTIVE), "images.Deployment.onbutton");
    	}
    	else {
    		imageOnOffRadioJavascript(htmlBuf, "reset", "1", "1", "0", "images.Deployment.onbutton");
    	}

    	htmlBuf.append("</td></tr>\n");

    	val= editor.getHostProfileMgr().getDlOrdSupport();
    	if(val==null || val.equals("")){
    		status=INACTIVE;
    	} else{
    		if(val.trim().equals(EBICSConstants.DEF_SHORT_TRUE)) {
    			status = ACTIVE;
    		} else if(val.trim().equals(EBICSConstants.DEF_SHORT_FALSE)){
    			status = INACTIVE;
    		}
    	}

//    	htmlBuf.append("<tr><td class='whiteStripe' colspan=2>\n");
//    	buildOnOffRadio(htmlBuf, "supportOrderDownload", langbean.getValue("Label.SupportOrderDownload"), String.valueOf(1),
//    			String.valueOf(status), "WizardInputRadio", "images.Deployment.onbutton");
//
//    	if (status != -1) {
//    		imageOnOffRadioJavascript(htmlBuf, "supportOrderDownload", String.valueOf(status),
//    				String.valueOf(ACTIVE), String.valueOf(INACTIVE), "images.Deployment.onbutton");
//    	}
//    	else {
//    		imageOnOffRadioJavascript(htmlBuf, "reset", "1", "1", "0", "images.Deployment.onbutton");
//    	}

    	htmlBuf.append("</td></tr>\n");

    	val= editor.getHostProfileMgr().getX509Support();
    	
    	if(val==null || val.equals("")){
    		status=ACTIVE;
    	}else{
    		if(val.trim().equals(EBICSConstants.DEF_SHORT_TRUE)) {
    			status = ACTIVE;
    		} else if(val.trim().equals(EBICSConstants.DEF_SHORT_FALSE)){
    			status = INACTIVE;
    		}
    	}

    	htmlBuf.append("<tr><td class='whiteStripe' colspan=2>\n");
//    	buildOnOffRadio(htmlBuf, "supportX509", langbean.getValue("Label.SupportX509"), String.valueOf(1),
//    			String.valueOf(status), "WizardInputRadio", "images.Deployment.onbutton");
//
//    	if (status != -1) {
//    		imageOnOffRadioJavascript(htmlBuf, "supportX509", String.valueOf(status),
//    				String.valueOf(ACTIVE), String.valueOf(INACTIVE), "images.Deployment.onbutton");
//    	}
//    	else {
//    		imageOnOffRadioJavascript(htmlBuf, "reset", "1", "1", "0", "images.Deployment.onbutton");
//    	}

    	htmlBuf.append("</td></tr>\n");


    	val= editor.getHostProfileMgr().getX509Persist();
    	if(val==null || val.equals("")){
    		status=ACTIVE;
    	}else{
    		if(val.trim().equals(EBICSConstants.DEF_SHORT_TRUE)) {
    			status = ACTIVE;
    		} else if(val.trim().equals(EBICSConstants.DEF_SHORT_FALSE)){
    			status = INACTIVE;
    		}
    	}

  //  	htmlBuf.append("<tr><td class='whiteStripe' colspan=2>\n");
//    	buildOnOffRadio(htmlBuf, "persistX509", langbean.getValue("Label.PersistX509"), String.valueOf(1),
//    			String.valueOf(status), "WizardInputRadio", "images.Deployment.onbutton");
//
//    	if (status != -1) {
//    		imageOnOffRadioJavascript(htmlBuf, "persistX509", String.valueOf(status),
//    				String.valueOf(ACTIVE), String.valueOf(INACTIVE), "images.Deployment.onbutton");
//    	}
//    	else {
//    		imageOnOffRadioJavascript(htmlBuf, "reset", "1", "1", "0", "images.Deployment.onbutton");
//    	}

    	htmlBuf.append("</td></tr>\n");
    	htmlBuf.append ("</table>\n");
    }

    private void buildBankURL(StringBuffer buf) {

    	// Get the Host URL value list in the vector
    	this.editor.getHostURLs();

    	// Constructs the list table to display the available BankURL values

    	buf.append("<table border=0 cellpadding=0 cellspacing=0 valign='top' width=475>\n");
    	EBICSBeanBase e = new EBICSBeanBase();
    	EBICSBeanBase.Paginator p = e.new Paginator(null,editor.getURLListPos(),editor.getURLListNum(),stepSize,6,new ArrayList(editor.bankUrlMgrVect),langbean,"./BankProfileWizard?wizType=" + wiz.wizType + "&wizObjType=" + wiz.objectType() + "&WizardAction=jump","wizard.bankURL");
    	editor.setURLListPos(p.getPos());
    	editor.setURLListNum(p.getNum());
    	stepSize = p.getStep();
    	List currentPageList = p.getCurrentPageList();
    	buf.append(p.getHeaderRow());
    	addHSpace(buf, "6");
    	buf.append( "<tr> ");
    	addVSpace(buf, "tblBorder", "1", "1");

    	buf.append( "<td width=75 class=opsTblHdr>&nbsp;</td>");
    	addVSpace(buf, "opsTblHdr", "1", "1");
    	addVSpace(buf, "opsTblHdr", "2", "1");
    	buf.append( "<td width=395 align='left' class=opsTblHdr>");

    	buf.append(langbean.getValue("wizard.bankURL"));
    	buf.append("&nbsp;</td>");
    	addVSpace(buf, "tblBorder", "1", "1");
    	buf.append( "</tr>\n");

    	addHSpace(buf, "6");

    	buf.append("<tr>");
    	addVSpace(buf, "tblBorder", "1", "1");
    	buf.append("<td class='bodyspacer' colspan=4 height='1'>");
    	makeImage(buf,"images.spacer",null,"1", null);
    	buf.append("</td>");
    	addVSpace(buf, "tblBorder", "1", "1");
    	buf.append("</tr>");

    	addHSpace(buf, "6");
    	String className = "editSelection";

    	buf.append("<tr>");
    	addVSpace(buf);
    	buf.append("<td align='right' valign='bottom' class='");
    	buf.append(className);
    	buf.append("'><a href=\"");
    	buf.append(getServlet());
    	buf.append("&WizardAction=Next&uriaction=add&");
    	buf.append(uniqueKey());
    	buf.append("\">");
    	makeImage(buf, "images.add", null, null, null, "align=absbottom vspace='2'");
    	buf.append("</a>");
    	buf.append("</td>");
    	addVSpace(buf);
    	addVSpace(buf, className, "2", "1");

    	buf.append("<td valign='top' class='");
    	buf.append(className);
    	buf.append("'>");
    	buf.append(langbean.getValue("wizard.bankURL.newURL"));
    	buf.append("</td>");
    	addVSpace(buf);
    	buf.append("</tr>\n");

    	className = "grayEditSelection";


    	if(editor.bankUrlMgrVect != null && editor.bankUrlMgrVect.size() != 0 ) {
    		for(int i = 0; i < currentPageList.size(); i++) {
    			editor.setCurrentHostUrlMgr((HostUrlMgr)currentPageList.get(i));
    			if (i>0) {
    				if (className.equals("editSelection")) {
    					className = "grayEditSelection";
    				} else{
    					className = "editSelection";
    				}
    			}

    			buf.append("<tr>");
    			addVSpace(buf);
    			buf.append("<td valign='bottom' align='center' class='");
    			buf.append(className);
    			buf.append("'>");
    			buf.append("<a href=\"");
    			buf.append(getServlet());
    			buf.append("&WizardAction=Next&uriaction=edit&CurrentURL=");
    			buf.append(UIGlobals.processHTMLDisplayValue(editor.currentHostUrlMgr().getHostURL()));
    			buf.append("&");
    			buf.append(uniqueKey());
    			buf.append("\">");
    			makeImage(buf, "images.edit", null, null, null, "align=absbottom");
    			buf.append("</a>&nbsp;");
    			buf.append("<a href=\"");
    			buf.append(getServlet());
    			buf.append("&WizardAction=Jump&uriaction=delete&CurrentURL=");
    			buf.append(UIGlobals.processHTMLDisplayValue(editor.currentHostUrlMgr().getHostURL()));
    			buf.append("&");
    			buf.append(uniqueKey());
    			buf.append("\">");
    			makeImage(buf, "images.delete", null, null, null, "align=absbottom");
    			buf.append("</a>");
    			buf.append("</td>");

    			addVSpace(buf);
    			addVSpace(buf, className, "2", "1");

    			buf.append("<td valign='top' class='");
    			buf.append(className);
    			buf.append("'>");
    			buf.append(UIGlobals.processHTMLDisplayValue(editor.currentHostUrlMgr().getHostURL()));
    			buf.append("</td>");
    			addVSpace(buf);
    			buf.append("</tr>\n");

    		}

    	}

    	addHSpace(buf, "6");
    	buf.append(p.getFooterRow());
    	buf.append("</table>\n");

    }

    private void buildBankProtocol(StringBuffer buf) {

    	// Get the Host Protocol value list in the vector
    	this.editor.getHostProtocols();

    	// Constructs the list table to display the available Bank protocol values

    	buf.append("<table border=0 cellpadding=0 cellspacing=0 valign='top' width=475>\n");
    	EBICSBeanBase e = new EBICSBeanBase();
    	EBICSBeanBase.Paginator p = e.new Paginator(null,editor.getProtocolListPos(),editor.getProtocolListNum(),stepSize,6,new ArrayList(editor.bankProtocolMgrVect),langbean,"./BankProfileWizard?wizType=" + wiz.wizType + "&wizObjType=" + wiz.objectType() + "&WizardAction=jump","wizard.bankProtocol");
    	editor.setProtocolListPos(p.getPos());
    	editor.setProtocolListNum(p.getNum());
    	stepSize = p.getStep();
    	List currentPageList = p.getCurrentPageList();
    	buf.append(p.getHeaderRow());
    	addHSpace(buf, "6");
    	buf.append( "<tr> ");
    	addVSpace(buf, "tblBorder", "1", "1");

    	buf.append( "<td width=75 class=opsTblHdr>&nbsp;</td>");
    	addVSpace(buf, "opsTblHdr", "1", "1");
    	addVSpace(buf, "opsTblHdr", "2", "1");
    	buf.append( "<td width=395 align='left' class=opsTblHdr>");

    	buf.append(langbean.getValue("wizard.bankProtocol"));
    	buf.append("&nbsp;</td>");
    	addVSpace(buf, "tblBorder", "1", "1");
    	buf.append( "</tr>\n");

    	addHSpace(buf, "6");

    	buf.append("<tr>");
    	addVSpace(buf, "tblBorder", "1", "1");
    	buf.append("<td class='bodyspacer' colspan=4 height='1'>");
    	makeImage(buf,"images.spacer",null,"1", null);
    	buf.append("</td>");
    	addVSpace(buf, "tblBorder", "1", "1");
    	buf.append("</tr>");

    	addHSpace(buf, "6");
    	String className = "editSelection";

    	buf.append("<tr>");
    	addVSpace(buf);
    	buf.append("<td align='right' valign='bottom' class='");
    	buf.append(className);
    	buf.append("'><a href=\"");
    	buf.append(getServlet());
    	buf.append("&WizardAction=Next&protoaction=add&");
    	buf.append(uniqueKey());
    	buf.append("\">");
    	makeImage(buf, "images.add", null, null, null, "align=absbottom vspace='2'");
    	buf.append("</a>");
    	buf.append("</td>");
    	addVSpace(buf);
    	addVSpace(buf, className, "2", "1");

    	buf.append("<td valign='top' class='");
    	buf.append(className);
    	buf.append("'>");
    	buf.append(langbean.getValue("wizard.bankProtocol.new"));
    	buf.append("</td>");
    	addVSpace(buf);
    	buf.append("</tr>\n");

    	className = "grayEditSelection";

        HashMap<String, Integer> pUsage = null;
        try {
            String hID = UIGlobals.processHTMLDisplayValue(this.editor.getHostProfileMgr().getHostID());
            pUsage = OfferMgrDBProxy.getBankProtocolUsage(hID);
        } catch (Exception eUsage) {
            pUsage = new HashMap<String, Integer>();
        }

    	if(editor.bankProtocolMgrVect!= null && editor.bankProtocolMgrVect.size() != 0 ) {
    		for(int i = 0; i < currentPageList.size(); i++) {
    			editor.setCurrentHostProtocolMgr((HostProtocolMgr)currentPageList.get(i));
    			if (i>0) {
    				if (className.equals("editSelection")) {
    					className = "grayEditSelection";
    				} else{
    					className = "editSelection";
    				}
    			}

    			if (UIGlobals.out.debug) {
    				UIGlobals.logDebug( "BankProfileBean.buildBankProtocol Protocol Usage map contains: " + pUsage);
    	            UIGlobals.logDebug( "BankProfileBean.buildBankProtocol Current Protocl selected is: " + editor.currentHostProtocolMgr().getProtocolValue());
    	        }
                Integer usage = pUsage.get(editor.currentHostProtocolMgr().getProtocolValue());
                boolean isInUse = usage != null && usage.intValue() > 0;

                if (UIGlobals.out.debug) {
    				UIGlobals.logDebug( "BankProfileBean.buildBankProtocol Usage returned: " + usage + " ; isInUse: " + isInUse);
    	        }
    			buf.append("<tr>");
    			addVSpace(buf);
    			buf.append("<td valign='bottom' align='center' class='");
    			buf.append(className);
    			buf.append("'>");
    			buf.append("<a href=\"");
    			buf.append(getServlet());
    			buf.append("&WizardAction=Next&protoaction=edit&CurrentProtocol=");
    			buf.append(UIGlobals.processHTMLDisplayValue(editor.currentHostProtocolMgr().getProtocolValue()));
    			buf.append("&");
    			buf.append(uniqueKey());
    			buf.append("\">");
    			makeImage(buf, "images.edit", null, null, null, "align=absbottom");
    			buf.append("</a>");
    			if (!isInUse) {
    			buf.append("&nbsp;<a href=\"");
    			buf.append(getServlet());
    			buf.append("&WizardAction=Jump&protoaction=delete&CurrentProtocol=");
    			buf.append(UIGlobals.processHTMLDisplayValue(editor.currentHostProtocolMgr().getProtocolValue()));
    			buf.append("&");
    			buf.append(uniqueKey());
    			buf.append("\">");
    			makeImage(buf, "images.delete", null, null, null, "align=absbottom");
    			buf.append("</a>");
    			}
    			buf.append("</td>");

    			addVSpace(buf);
    			addVSpace(buf, className, "2", "1");

    			buf.append("<td valign='top' class='");
    			buf.append(className);
    			buf.append("'>");
    			buf.append(UIGlobals.processHTMLDisplayValue(editor.currentHostProtocolMgr().getProtocolValue()));
    			buf.append("</td>");
    			addVSpace(buf);
    			buf.append("</tr>\n");
    		}
    	}
    	addHSpace(buf, "6");
    	buf.append(p.getFooterRow());
    	buf.append("</table>\n");
    }

    private void buildBankProcess(StringBuffer buf) {
    	// Get the Host Process value list in the vector
    	this.editor.getHostProcesses();

    	// Constructs the list table to display the available Bank process values

    	buf.append("<table border=0 cellpadding=0 cellspacing=0 valign='top' width=475>\n");
    	EBICSBeanBase e = new EBICSBeanBase();
    	EBICSBeanBase.Paginator p = e.new Paginator(null,editor.getProcessListPos(),editor.getProcessListNum(),stepSize,6,new ArrayList(editor.bankVerMgrVect),langbean,"./BankProfileWizard?wizType=" + wiz.wizType + "&wizObjType=" + wiz.objectType() + "&WizardAction=jump","wizard.bankProcess");
    	editor.setProcessListPos(p.getPos());
    	editor.setProcessListNum(p.getNum());
    	stepSize = p.getStep();
    	List currentPageList = p.getCurrentPageList();
    	buf.append(p.getHeaderRow());
    	addHSpace(buf, "6");
    	buf.append( "<tr> ");
    	addVSpace(buf, "tblBorder", "1", "1");

    	buf.append( "<td width=75 class=opsTblHdr>&nbsp;</td>");
    	addVSpace(buf, "opsTblHdr", "1", "1");
    	addVSpace(buf, "opsTblHdr", "2", "1");
    	buf.append( "<td width=395 align='left' class=opsTblHdr>");

    	buf.append(langbean.getValue("wizard.bankProcess"));
    	buf.append("&nbsp;</td>");
    	addVSpace(buf, "tblBorder", "1", "1");
    	buf.append( "</tr>\n");

    	addHSpace(buf, "6");

    	buf.append("<tr>");
    	addVSpace(buf, "tblBorder", "1", "1");
    	buf.append("<td class='bodyspacer' colspan=4 height='1'>");
    	makeImage(buf,"images.spacer",null,"1", null);
    	buf.append("</td>");
    	addVSpace(buf, "tblBorder", "1", "1");
    	buf.append("</tr>");

    	addHSpace(buf, "6");
    	String className = "editSelection";

    	buf.append("<tr>");
    	addVSpace(buf);
    	buf.append("<td align='right' valign='bottom' class='");
    	buf.append(className);
    	buf.append("'><a href=\"");
    	buf.append(getServlet());
    	buf.append("&WizardAction=Next&processAction=add&");
    	buf.append(uniqueKey());
    	buf.append("\">");
    	makeImage(buf, "images.add", null, null, null, "align=absbottom vspace='2'");
    	buf.append("</a>");
    	buf.append("</td>");
    	addVSpace(buf);
    	addVSpace(buf, className, "2", "1");

    	buf.append("<td valign='top' class='");
    	buf.append(className);
    	buf.append("'>");
    	buf.append(langbean.getValue("wizard.bankProcess.new"));
    	buf.append("</td>");
    	addVSpace(buf);
    	buf.append("</tr>\n");

    	className = "grayEditSelection";

        HashMap<String, Integer> pUsage = null;
        try {
            String hID = UIGlobals.processHTMLDisplayValue(this.editor.getHostProfileMgr().getHostID());
            pUsage = OfferMgrDBProxy.getBankProcessUsage(hID);
        } catch (Exception eUsage) {
            pUsage = new HashMap<String, Integer>();
        }

    	if(editor.bankVerMgrVect != null && editor.bankVerMgrVect.size() != 0 ) {
    		for(int i = 0; i < currentPageList.size(); i++) {
    			HostVerMgr hostVerMgr = (HostVerMgr)currentPageList.get(i);
    			if (i>0) {
    				if (className.equals("editSelection")) {
    					className = "grayEditSelection";
    				} else{
    					className = "editSelection";
    				}
    			}

                Integer usage = pUsage.get(hostVerMgr.getVerType() + "." + hostVerMgr.getVerValue());
                boolean isInUse = usage != null && usage.intValue() > 0;

    			buf.append("<tr>");
    			addVSpace(buf);
    			buf.append("<td valign='bottom' align='center' class='");
    			buf.append(className);
    			buf.append("'>");
    			if (!isInUse) { // Can not edit or delete if it is in use
    			buf.append("<a href=\"");
    			buf.append(getServlet());
    			buf.append("&WizardAction=Next&processAction=edit&CurrentType=");
    			buf.append(UIGlobals.processHTMLDisplayValue(hostVerMgr.getVerType()));
    			buf.append("&CurrentVer=");
    			buf.append(UIGlobals.processHTMLDisplayValue(hostVerMgr.getVerValue()));
    			buf.append("&");
    			buf.append(uniqueKey());
    			buf.append("\">");
    			makeImage(buf, "images.edit", null, null, null, "align=absbottom");
    			buf.append("</a>&nbsp;");
    			buf.append("<a href=\"");
    			buf.append(getServlet());
    			buf.append("&WizardAction=Jump&processAction=delete&CurrentType=");
    			buf.append(UIGlobals.processHTMLDisplayValue(hostVerMgr.getVerType()));
    			buf.append("&CurrentVer=");
    			buf.append(UIGlobals.processHTMLDisplayValue(hostVerMgr.getVerValue()));
    			buf.append("&");
    			buf.append(uniqueKey());
    			buf.append("\">");
    			makeImage(buf, "images.delete", null, null, null, "align=absbottom");
    			buf.append("</a>");
    			}
    			buf.append("</td>");

    			addVSpace(buf);
    			addVSpace(buf, className, "2", "1");

    			buf.append("<td valign='top' class='");
    			buf.append(className);
    			buf.append("'>");
    			buf.append(UIGlobals.processHTMLDisplayValue(editor.process.getName(hostVerMgr.getVerType())+"("+hostVerMgr.getVerValue()+")"));
    			buf.append("</td>");
    			addVSpace(buf);
    			buf.append("</tr>\n");
    		}
    	}
    	addHSpace(buf, "6");
    	buf.append(p.getFooterRow());
    	buf.append("<tr><td><input name='isActionNext' value='No' type='hidden'></td></tr>");
    	buf.append("</table>\n");
    }


    private void buildBankURLInfo(StringBuffer buf) {

    	String val = "";
    	String validFromDate ="";

    	if(editor.currentHostUrlMgr()!=null && editor.URIAction!=null && editor.URIAction.equalsIgnoreCase("edit")){
    		val = editor.currentHostUrlMgr().getHostURL();
    		YTimestamp ts = (YTimestamp)editor.currentHostUrlMgr().getURLValidFrom();

    		if(ts != null){
    			validFromDate = ts.getString("MM/dd/yyyy");
    		}
    	}
    	buf.append("<table>\n");
    	buildInputBox(buf,langbean.getValue("wizard.bankURL"),  "URLName", val, "45", "220",
    			"<td valign=\"bottom\" class='WizardDeploymentText' nowrap>",
    			"</td><td valign=\"bottom\" class=\"WizardInputData\">",
    	"</td>");
    	buf.append("</tr>\n");


    	buf.append("<tr>");
    	buf.append("<td valign=\"bottom\" nowrap class='WizardInputText'>");
    	buf.append(langbean.getValue("wizard.validFrom"));
    	buf.append(":");
    	buf.append("</td>");
    	buf.append("<td nowrap>");
    	buf.append("<input readonly=\"true\" type=text name=\"vDate\"  value=\"");
    	buf.append(validFromDate);
    	buf.append("\" size=19 maxlength=19>");
    	buf.append("&nbsp;");
    	buildDatePickerButton
    	(
    			buf,
    			"cal2",
    			"wizform",
    			"vDate",
    			false,
    			UIBeanBase.DATE_FORMAT_2
    	);
    	buf.append("<tr><td/><td colspan=\"4\">&nbsp;&nbsp;<span class=\"info\">" );
    	buf.append(UIGlobals.encodeHTMLString(
                  	 langbean.getValue("Label.eb.DateEntryPattern")));
    	buf.append("</span></td></tr>");
    	buf.append("<tr><td><input name='isNextOnURLinfo' value='No' type='hidden'></td></tr>");

    	buf.append("</table>\n");
    }

    private void  buildBankProtocolInfo(StringBuffer buf) {

    	StringBuffer jsValidate = new StringBuffer();
    	String val = "";
    	String val1 = "";
    	String skey = "";
    	String sval = "";
    	int count =0;
    	StringTokenizer st = null;
        boolean isEdit = false;

    	if(editor.currentHostProtocolMgr()!=null && editor.protocolAction!=null &&  editor.protocolAction.equalsIgnoreCase("edit")){
    		val = editor.currentHostProtocolMgr().getProtocolValue();
    		val1 = editor.currentHostProtocolMgr().getReleaseVer();
            isEdit = true;
    	}

    	// Get a list of Protocol
    	List<String> protocolList = EBICSUtils.getProtocolList();

    	buf.append("<table>\n");
    	buf.append("<tr><td colspan=\"2\"></td></tr>\n");
    	buf.append("<tr>\n");
    	buf.append("<td id='protocolname' width=215 valign=\"bottom\" class=\"");
        buf.append("Wizard"+wiz.location+"Text");
        buf.append("\">");
        buf.append(langbean.getValue("wizard.protocolVer"));
        buf.append(": </td>");
        buf.append ("<td valign='bottom' class='WizardInputData'>\n");
        buf.append ("<div id=\"select_list0");
        buf.append ("\">");
        buf.append ("<select  name='protocolVersion' onChange=\"populateReleaseVer(this.options[this.selectedIndex].value);\"");
        if (isEdit) buf.append(" disabled");
        buf.append(">\n");

        if (protocolList.size() == 0) {
            buf.append("<option value=''");
            buf.append(">");
            buf.append(langbean.getValue("Label.NoneAvailable"));
            buf.append("</option>\n");
          } else {
        	  count = 0;
    		  for (String protocol: protocolList) {
    			  st = new StringTokenizer(protocol, "|");
    			  skey = st.nextToken();
    			  sval = st.nextToken();
	              // generate the javascript for combo list select
	              jsValidate.append("arrReleaseVer[" + count + "] = new Object();\n");
	              jsValidate.append("arrReleaseVer[" + count + "].protocolver = '" + UIGlobals.processHTMLDisplayValue(skey) +"';\n");
	              jsValidate.append("arrReleaseVer[" + count + "].releasever = '" + UIGlobals.processHTMLDisplayValue(sval) +"';\n");

	              if (count == 0 && (val1 == null || val1.trim().length() == 0)) {
	            	  val1 = sval; // set default
	              }

	          	  buf.append("<option value=\"" + skey + "\"");
	          	  if (skey.equals(val)) {
	                    buf.append(" SELECTED");
	              }
	          	  buf.append(">");
	          	  buf.append(UIGlobals.processHTMLDisplayValue(skey));
	              buf.append("</option>\n");
	              count++;
            }
      	}
        buf.append ("</select></td>");
        buf.append ("</tr><tr>\n");

        // Release Version
        buf.append ("<td valign=\"bottom\" class=\"WizardInputText\">"+langbean.getValue("wizard.releaseVer"));
        buf.append (": </td><td valign=\"bottom\" nowrap class=\"WizardInputData\">");
        buf.append ("<input type='text' name='releaseVersion' readonly='true' value='"+val1+"' size=5 maxlength=5></td>");

    	buf.append("</tr><tr><td colspan=2>&nbsp;</td></tr>\n");
    	buf.append("<tr><td><input name='isNextOnProtocolInfo' value='No' type='hidden'></td></tr>");
    	buf.append("</table>\n");

    	buf.append ("<script language=\"JavaScript\">\n");
    	buf.append (" var i = 0;\n");
    	buf.append (" var arrReleaseVer = new Array(");
        buf.append (protocolList.size());
        buf.append (");\n");
        buf.append (jsValidate.toString()); // put in the jsValidate

        buf.append ("function populateReleaseVer(selected){\n");
        buf.append ("  if(selected.indexOf('next') != -1)\n");
        buf.append ("    document.wizform.submit();\n");
        buf.append ("  else {\n");
        buf.append ("     var tmp = eval( \"document.wizform\");\n");
        buf.append ("     for (i=0; i < arrReleaseVer.length; i++) {\n");
        buf.append ("       if (arrReleaseVer[i].protocolver == selected) {\n");
        buf.append ("         tmp.releaseVersion.value = arrReleaseVer[i].releasever;\n");
        buf.append ("       }\n");
        buf.append ("     }\n");
        buf.append ("  }\n");
        buf.append ("}\n");
        buf.append ("</script>\n");
    }

    private void  buildBankProcessInfo(StringBuffer buf) {

    	String val = "";
    	String val1 = "";
    	Vector<String> vecProcessVer = null;
    	int count = 0, i = 0;
    	StringBuffer jsValidate = new StringBuffer();
    	String skey = "";

    	if(editor.currentHostVerMgr()!=null && editor.processAction!=null &&  editor.processAction.equalsIgnoreCase("edit")){
    		//val = editor.process.getName(editor.currentHostVerMgr().getVerType());
    		val = editor.currentHostVerMgr().getVerType();
    		val1 = editor.currentHostVerMgr().getVerValue();
    	}

    	// Get a list of Protocol
    	HashMap hm = EBICSUtils.getProcessList();

    	buf.append("<table>\n");
    	buf.append("<tr><td colspan=\"2\"></td></tr>\n");
    	buf.append("<tr>\n");
    	buf.append("<td id='processType' width=215 valign=\"bottom\" class=\"");
        buf.append("Wizard"+wiz.location+"Text");
        buf.append("\">");
        buf.append(langbean.getValue("wizard.processType"));
        buf.append(": </td>");
        buf.append ("<td valign='bottom' class='WizardInputData'>\n");
        buf.append ("<div id=\"select_list0");
        buf.append ("\">");
        buf.append ("<select  name='processType' onChange=\"populateProcessVer(this.options[this.selectedIndex].value);\"");
        buf.append(">\n");

        if (hm.size() == 0) {
            buf.append("<option value=''");
            buf.append(">");
            buf.append(langbean.getValue("Label.NoneAvailable"));
            buf.append("</option>\n");
          } else {
        	  count = 0;
        	  Iterator iter = hm.keySet().iterator();
        	   while (iter.hasNext()) {
        		  skey = (String)iter.next();
        		  vecProcessVer = (Vector<String>) hm.get(skey);

        		  // generate the javascript for combo list select
        		  jsValidate.append(" arrProcessVer["+ count +"] = new Object();\n");
        		  jsValidate.append(" arrProcessVer["+ count +"].processType = '"+skey+"';\n");
        		  jsValidate.append(" arrProcessVer["+ count +"].process = new Array("+vecProcessVer.size()+");\n");

        		  for (i=0; i < vecProcessVer.size(); i++) {
        			  jsValidate.append(" arrProcessVer["+ count +"].process["+ i +"] = new Object();\n");
        			  jsValidate.append(" arrProcessVer["+ count +"].process["+ i +"].processVer = '"+vecProcessVer.get(i)+"'\n");
        		  }

	              if (count == 0 && (val == null || val.trim().length() == 0)) {
	            	  val = skey; // set default
	            	  if (vecProcessVer.size() > 0)
              	  	    val1 = (String)vecProcessVer.get(count); // set default
	              }

	          	  buf.append("<option value=\"" + skey + "\"");
	          	  if (skey.equals(val)) {
	                    buf.append(" SELECTED");
	              }
	          	  buf.append(">");
	          	  buf.append(UIGlobals.processHTMLDisplayValue(EBICSUtils.getProcessDescr(langbean).get(skey)));
	              buf.append("</option>\n");
	              count++;
            }
      	}
        buf.append ("</select></td>");
        buf.append ("</tr><tr>\n");

        buf.append ("<tr><td width=215 valign='bottom' class='WizardDeploymentText'>\n");
        buf.append (langbean.getValue("wizard.processVersion") + ":</td>");
        buf.append ("<td valign='bottom' class='WizardInputData'>");
        buf.append ("<select  name='processVer'>");
        buf.append ("</select></td>");

    	buf.append("</tr><tr><td colspan=2>&nbsp;</td></tr>\n");
    	buf.append("<tr><td><input name='isNextOnProcessInfo' value='No' type='hidden'></td></tr>");
    	buf.append("</table>\n");

    	buf.append ("<script language=\"JavaScript\">\n");
    	buf.append (" var arrProcessVer = new Array(");
        buf.append (hm.size());
        buf.append (");\n");
        buf.append (jsValidate.toString() + "\n"); // put in the jsValidate

        buf.append ("function populateProcessVer(selected){\n");
        buf.append ("  var tmp = eval( \"document.wizform\");\n");
        buf.append ("  var i = 0, j=0;\n");

        buf.append ("  if(selected.indexOf('next') != -1)\n");
        buf.append ("    document.wizform.submit();\n");
        buf.append ("  else {\n");
        buf.append ("   for ( i=0; i < arrProcessVer.length; i++) {\n");
        buf.append ("    if (arrProcessVer[i].processType == selected) {\n");
        buf.append ("       for (j = 0; j < arrProcessVer[i].process.length; j++) {\n");
        buf.append ("          tmp.processVer.options.length = arrProcessVer[i].process.length;\n");
        buf.append ("          tmp.processVer.options[j] = new Option(arrProcessVer[i].process[j].processVer, arrProcessVer[i].process[j].processVer, false, false);\n");
        buf.append ("          if (arrProcessVer[i].process[j].processVer == '"+val1+"') {\n");
        buf.append ("              tmp.processVer.options[j].selected = true;\n");
        buf.append ("          }\n");
        buf.append ("       }\n");
        buf.append ("    }\n");
        buf.append ("   }\n");
        buf.append ("  }\n");
        buf.append ("}\n\n");

		buf.append ("function initScreen(){\n");
		buf.append ("  populateProcessVer('"+val+"');\n");
		buf.append ("}\n");
		buf.append ("initScreen();\n");

        buf.append ("</script>\n");
    }

    private void buildConfirm(StringBuffer buf) {

    	String yesLabel = langbean.getValue("Yes");
    	String noLabel = langbean.getValue("No");

    	buf.append("<table cellpadding=0 cellspacing=0 border=0 width=520>\n");
    	buf.append("<tr>");
    	buf.append("</td>\n");
    	buf.append("</tr>\n");

    	buf.append ("<tr>");
		startStripeCell (buf, "WizardInputText", "colspan=4");
		if ( page.activity.equals("bankConfirmDelete")) {
			makeImage(buf, "images.alert", null, null, null);
			buf.append(langbean.getValue("Msg.MainDeleteWarning"));
			buf.append("<br><br>");
			buf.append(langbean.getValue("Label.BannkProfileConfig.ConfirmDelete"));
			buf.append("</td>\n");
			buf.append("</tr>\n");
		} else {
			buf.append (langbean.getValue ("Label.BankProfileConfig.Settings"));
			buf.append ("</td>\n");
			buf.append ("</tr>\n");
		}

    	addHSpace(buf, "4");
    	addHSpace(buf, "4", null, "1");
    	addHSpace(buf, "4");

    	// BANK YFS INFO
    	buildConfirmLine(buf, langbean.getValue("Label.BankID"), UIGlobals
    			.processHTMLDisplayValue(editor.getHostProfileMgr().getHostID()));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("Label.BankName"), UIGlobals
    			.processHTMLDisplayValue(editor.getHostProfileMgr().getFirstName()));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("CP.Address1"), UIGlobals
    			.processHTMLDisplayValue( editor.getHostProfileMgr().getAddressLine1()));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("CP.Address2"), UIGlobals
    			.processHTMLDisplayValue( editor.getHostProfileMgr().getAddressLine2()));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("CP.City"), UIGlobals
    			.processHTMLDisplayValue( editor.getHostProfileMgr().getCity()));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("CP.State"), UIGlobals
    			.processHTMLDisplayValue( editor.getHostProfileMgr().getState()));
    	addHSpace(buf, "4", "ltspacer", "1");
    	String countryName = DigiCertBean.getCountries(langbean).getName(editor.getHostProfileMgr().getCountry());
    	buildConfirmLine(buf, langbean.getValue("CP.Country"), UIGlobals.processHTMLDisplayValue(langbean.getValue(countryName)));
    	addHSpace(buf, "4", "ltspacer", "1");
    	String timezone = editor.getHostProfileMgr().getTimeZone();
    	buildConfirmLine(buf, langbean.getValue("CP.TimeZone"), (timezone!=null && timezone.trim().length()>0) ?UIGlobals
    			.processHTMLDisplayValue(langbean.getValue(EBICSConstants.EBICS_TAG+EBICSConstants.EBICS_TIMEZONE_DELIMITER+timezone)): null);
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("CP.Zip"), UIGlobals
    			.processHTMLDisplayValue( editor.getHostProfileMgr().getZipCode()));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("CP.EmailAddress"), UIGlobals
    			.processHTMLDisplayValue( editor.getHostProfileMgr().getEmailID()));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("CP.PhoneNumber"), UIGlobals
    			.processHTMLDisplayValue( editor.getHostProfileMgr().getDayPhone()));
    	addHSpace(buf, "4", "ltspacer", "1");

//    	// BANK INFO
//    	buildConfirmLine(buf, langbean.getValue("Label.EncPriCert"), UIGlobals
//    			.processHTMLDisplayValue( sysCertList.getName(editor.getHostProfileMgr().getEncrPrivKeyID())));
//    	addHSpace(buf, "4", "ltspacer", "1");
//    	buildConfirmLine(buf, langbean.getValue("Label.EncPubCert"), UIGlobals
//    			.processHTMLDisplayValue( trustedCertList.getName(editor.getHostProfileMgr().getEncrPubKeyID())));
    	
    	// X509
    	if (editor.getHostProfileMgr().getCertOption().equalsIgnoreCase("X509")) {
    	buildConfirmLine(buf, langbean.getValue("Label.EncPriCert"), UIGlobals
    			.processHTMLDisplayValue( sysCertList.getName(editor.getHostProfileMgr().getEncrPrivKeyID())));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("Label.EncPubCert"), UIGlobals
    			.processHTMLDisplayValue( trustedCertList.getName(editor.getHostProfileMgr().getEncrPubKeyID())));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("Label.AuthPriCert"), UIGlobals
    			.processHTMLDisplayValue( sysCertList.getName(editor.getHostProfileMgr().getAuthPrivKeyID())));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("Label.AuthPubCert"), UIGlobals
    			.processHTMLDisplayValue( trustedCertList.getName(editor.getHostProfileMgr().getAuthPubKeyID())));
    	addHSpace(buf, "4", "ltspacer", "1");
    	}
    	
    	//RSA
    	else if(editor.getHostProfileMgr().getCertOption().equalsIgnoreCase("keys")) {
    	buildConfirmLine(buf, langbean.getValue("Label.EncPriCert"), UIGlobals
    			.processHTMLDisplayValue( editor.getHostProfileMgr().getEncrRSAPrivKeyName()));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("Label.EncPubCert"), UIGlobals
    			.processHTMLDisplayValue( editor.getHostProfileMgr().getEncrRSAPubKeyName()));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("Label.AuthPriCert"), UIGlobals
    			.processHTMLDisplayValue(editor.getHostProfileMgr().getAuthRSAPrivKeyName()));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("Label.AuthPubCert"), UIGlobals
    			.processHTMLDisplayValue( editor.getHostProfileMgr().getAuthRSAPubKeyName()));
    	}
    	
    	//End
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("Label.MAXRecovery"), UIGlobals
    			.processHTMLDisplayValue( Long.toString(editor.getHostProfileMgr().getMaxRecoveryNum())));
    	addHSpace(buf, "4", "ltspacer", "1");
    	   	
    	buildConfirmLine(buf, langbean.getValue("Label.MaxSignAllowed"), UIGlobals
    			.processHTMLDisplayValue( Long.toString(editor.getHostProfileMgr().getMaxSigAllowed())));
    	addHSpace(buf, "4", "ltspacer", "1");
    	
    	buildConfirmLine(buf, langbean.getValue("Label.Prevalidation"), UIGlobals
    			.processHTMLDisplayValue( (editor.getHostProfileMgr().getPrevalidate()).trim().equals(EBICSConstants.DEF_SHORT_TRUE)? yesLabel : noLabel));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("Label.SupportClientDownload"), UIGlobals
    			.processHTMLDisplayValue( (editor.getHostProfileMgr().getClntDLSupport()).trim().equals(EBICSConstants.DEF_SHORT_TRUE)? yesLabel : noLabel));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("Label.SupportOrderDownload"), UIGlobals
    			.processHTMLDisplayValue( (editor.getHostProfileMgr().getDlOrdSupport()).trim().equals(EBICSConstants.DEF_SHORT_TRUE)? yesLabel : noLabel));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("Label.SupportX509"), UIGlobals
    			.processHTMLDisplayValue( (editor.getHostProfileMgr().getX509Support()).trim().equals(EBICSConstants.DEF_SHORT_TRUE)? yesLabel : noLabel));
    	addHSpace(buf, "4", "ltspacer", "1");
    	buildConfirmLine(buf, langbean.getValue("Label.PersistX509"), UIGlobals
    			.processHTMLDisplayValue( (editor.getHostProfileMgr().getX509Persist()).trim().equals(EBICSConstants.DEF_SHORT_TRUE)? yesLabel : noLabel));
    	addHSpace(buf, "4", "ltspacer", "1");

    	// BANK URL
    	StringBuffer bankUrlLink = new StringBuffer("");
    	String wiztype = editor.getActionType() == BankProfileEditor.DELETE ? "DeleteBankProfile" :"BankProfile";
    	if (editor.getHostURLs() != null && editor.getHostURLs().size() > 0) {
    		bankUrlLink.append("<a class='selectResource' href=\"javascript:openSizableSubProcessWin('./Page?");
	    	bankUrlLink.append("next=page.bankurlinfo&undo=false&isConfirmPage=true&pos=0&num=15&" + WizardBase.WIZARD_TYPE + "=" + wiztype + "&" + WizardBase.WIZARD_OBJECT_TYPE + "=" + WizardObject.getWizardObjectType("BANKPROFILE"));
	    	bankUrlLink.append("&" + uniqueKey());
	    	bankUrlLink.append("','picker','650','500')\">");
	    	bankUrlLink.append(langbean.getValue("Label.ShowAll"));
	        bankUrlLink.append("</a>\n");
    	}
    	buildConfirmLine(buf, langbean.getValue("Label.BankURLConfig"), bankUrlLink.toString());
    	addHSpace(buf, "4", "ltspacer", "1");

    	// BANK PROTOCOL
    	StringBuffer bankProtLink = new StringBuffer("");
    	if (editor.getHostProtocols() != null && editor.getHostProtocols().size() > 0) {
    		bankProtLink.append("<a class='selectResource' href=\"javascript:openSizableSubProcessWin('./Page?");
	    	bankProtLink.append("next=page.bankprotocolinfo&undo=false&isConfirmPage=true&pos=0&num=15&" + WizardBase.WIZARD_TYPE + "=" + wiztype + "&" + WizardBase.WIZARD_OBJECT_TYPE + "=" + WizardObject.getWizardObjectType("BANKPROFILE"));
	    	bankProtLink.append("&" + uniqueKey());
	    	bankProtLink.append("','picker','650','500')\">");
	    	bankProtLink.append(langbean.getValue("Label.ShowAll"));
	    	bankProtLink.append("</a>\n");
    	}
    	buildConfirmLine(buf, langbean.getValue("Label.BankProtocolConfig") + "/" + langbean.getValue("Label.BankProcessConfig"), bankProtLink.toString());
    	addHSpace(buf, "4", "ltspacer", "1");

    	addHSpace(buf, "4");
    	addHSpace(buf, "4", null, "20");
    	buf.append("</table>\n");
    }

    /**
     * showBankUrlInfo - Bank URL Info Details
     */
    public String showBankUrlInfo(HttpSession session, HttpServletRequest request) {
    	boolean confirmPage = false;
    	String strConfirmPage = request.getParameter("isConfirmPage");
    	if (YFCObject.isVoid(strConfirmPage)) {
    		confirmPage = false;
    	} else if (YFCObject.equals("true", strConfirmPage)) {
    		confirmPage = true;
    	}
    	Vector<HostUrlMgr> vect = null;
    	String hostId = null;
    	try {
    		initInfo(session, request);

    		if (editor == null)
    			editor = new BankProfileEditor(WizardObject.getWizardObjectType("BANKPROFILE"));

    		if (confirmPage) {
    			vect = editor.getHostURLs();
    		} else {
	    		hostId = request.getParameter("host");
	    		HostProfileMgr hpMgr = ProfileMgrDBProxy.readHostMgr(hostId, false, true);
	    		vect = hpMgr.getHostURL();
    		}
    	} catch (Exception e) {}

    	SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
    	StringBuffer buf = new StringBuffer();
    	String className = null;
    	HostUrlMgr hurlmgr = null;
    	if (vect != null && vect.size() > 0) {
    		buf.append("<table border=0 cellpadding=0 cellspacing=0 valign='top' width=500>\n");
    		EBICSBeanBase e = new EBICSBeanBase();
    		EBICSBeanBase.Paginator p = e.new Paginator(request,pos,num,stepSize,7,new ArrayList(vect),langbean,"./Page?next=page.bankurlinfo&isConfirmPage="+confirmPage+"&host="+hostId,"wizard.bankURL");
    		pos = p.getPos();
    		num = p.getNum();
    		stepSize = p.getStep();
    		List currentPageList = p.getCurrentPageList();
    		buf.append(p.getHeaderRow());
    		addHSpace(buf,"40");
            buf.append( "<tr> ");
            buildTitle(buf, langbean.getValue("wizard.bankURL"), 250);
            buildTitle(buf, 
			langbean.getValue("wizard.validFrom")+
			"  "+
			UIGlobals.encodeHTMLString(
				langbean.getValue("Label.eb.DateEntryPattern")),
			250
			);
            addVSpace(buf, "tblBorder", "1", "1");
            buf.append( "</tr>\n");

            addHSpace(buf, "40");
            className = "grayEditSelection";
    		for(int i = 0; i < currentPageList.size(); i++) {
    			hurlmgr = (HostUrlMgr)currentPageList.get(i);
    	   		if (i>0) {
    	   			if (className.equals("editSelection")) {
    	   				className = "grayEditSelection";
    	   			} else{
    	   				className = "editSelection";
    	   			}
    	   		}

	     		buf.append("<tr>");
	            buildLine(buf,className,UIGlobals.processHTMLDisplayValue(hurlmgr.getHostURL()),"left");
	            if (hurlmgr.getURLValidFrom() != null) {
	            	buildLine(buf,className,UIGlobals.processHTMLDisplayValue(formatter.format(new Date(hurlmgr.getURLValidFrom().getTime()))),"center");
	            } else {
	            	buildLine(buf,className,"","center");
	            }
	            addVSpace(buf, "tblBorder", "1", "1");
	            buf.append("</tr>\n");
	    	}
    		addHSpace(buf, "40");
    		buf.append(p.getFooterRow());
    		buf.append("</table>\n");
    	}


        return buf.toString();
    }

    /**
     * showBankProtocolInfo - Bank Protocol Info Details
     */
    public String showBankProtocolInfo(HttpSession session, HttpServletRequest request) {
    	boolean confirmPage = false;
    	String strConfirmPage = request.getParameter("isConfirmPage");
    	if (YFCObject.isVoid(strConfirmPage)) {
    		confirmPage = false;
    	} else if (YFCObject.equals("true", strConfirmPage)) {
    		confirmPage = true;
    	}
    	Vector<HostProtocolMgr> vect = null;
    	String hostId = null;
    	try {
    		initInfo(session, request);

    		if (editor == null)
    			editor = new BankProfileEditor(WizardObject.getWizardObjectType("BANKPROFILE"));

    		if (confirmPage) {
    			vect = editor.getHostProtocols();
    		} else {
    			hostId = request.getParameter("host");
    			HostProfileMgr hpMgr = ProfileMgrDBProxy.readHostMgr(hostId, true, false);
    			vect = hpMgr.getHostProtocol();
    		}
    	} catch (Exception e) {}



    	SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
    	StringBuffer buf = new StringBuffer();
    	String className = null;
    	String longForm = null;
    	HostProtocolMgr hpromgr = null;
    	if (vect != null && vect.size() > 0) {
    		buf.append("<table border=0 cellpadding=0 cellspacing=0 valign='top'>\n");
    		EBICSBeanBase e = new EBICSBeanBase();
    		EBICSBeanBase.Paginator p = e.new Paginator(request,pos,num,stepSize,8,new ArrayList(vect),langbean,"./Page?next=page.bankprotocolinfo&isConfirmPage="+confirmPage+"&host="+hostId,"wizard.protocolVer");
    		pos = p.getPos();
    		num = p.getNum();
    		stepSize = p.getStep();
    		List currentPageList = p.getCurrentPageList();
    		buf.append(p.getHeaderRow());
            addHSpace(buf, "40");
            buf.append( "<tr> ");
            buildTitle(buf, langbean.getValue("wizard.protocolVer"), 80);
            buildTitle(buf, langbean.getValue("wizard.releaseVer"), 80);

            // Inner Table
            buf.append("<td>");
    		buf.append("<table border=0 cellpadding=0 cellspacing=0 valign='top'>\n");
    		addHSpace(buf, "40");
    		buf.append( "<tr> ");
            buildTitle(buf, langbean.getValue("wizard.bankProcess"), 0, 4, 1);
            buf.append("</tr>");
            addHSpace(buf, "40");
    		buf.append( "<tr> ");
            buildTitle(buf, langbean.getValue("wizard.processType"), 80);
            buildTitle(buf, langbean.getValue("wizard.processVersion"), 80);
            buf.append("</tr>");
            buf.append("</table>");

            buf.append("</td>");
            addVSpace(buf, "tblBorder", "1", "1");
            buf.append( "</tr>\n");


            addHSpace(buf, "40");
            className = "grayEditSelection";
            for(int i = 0; i < currentPageList.size(); i++) {
                hpromgr = (HostProtocolMgr)currentPageList.get(i);
    	   		if (i>0) {
    	   			if (className.equals("editSelection")) {
    	   				className = "grayEditSelection";
    	   			} else{
    	   				className = "editSelection";
    	   			}
    	   		}

	     		buf.append("<tr>");
	     		buildLine(buf,className,UIGlobals.processHTMLDisplayValue(hpromgr.getProtocolValue()),"center");
	     		buildLine(buf,className,UIGlobals.processHTMLDisplayValue(hpromgr.getReleaseVer()),"center");
	     		Vector<HostVerMgr> hostVect = hpromgr.getAllHostVer();
	            buf.append("<td>");
        		buf.append("<table border=0 cellpadding=0 cellspacing=0 valign='top'>\n");
        		for (int j=0; j<hostVect.size(); j++) {
	     			HostVerMgr ver = hostVect.get(j);
	        		buf.append( "<tr> ");
	        		if (ver.getVerType().equals("E")) longForm = "Encryption";
	        		else if (ver.getVerType().equals("A")) longForm = "Authentication";
	        		else if (ver.getVerType().equals("S")) longForm = "Signature";

		     		buildLine(buf, className, UIGlobals.processHTMLDisplayValue(longForm), 80, 1, 1);
		     		buildLine(buf, className, UIGlobals.processHTMLDisplayValue(ver.getVerValue()), 80, 1, 1);
	                buf.append("</tr>");
	     		}
	     		if (hostVect.size() == 0) {
	        		buf.append( "<tr> ");
		     		buildLine(buf, className, "", 80, 1, 1);
		     		buildLine(buf, className, "", 80, 1, 1);
	                buf.append("</tr>");
	     		}
                buf.append("</table>");

	            buf.append("</td>");
	            addVSpace(buf, "tblBorder", "1", "1");
	            buf.append( "</tr>\n");
	    	}
    		addHSpace(buf,"40");
    		buf.append(p.getFooterRow());
    	}
        buf.append("</table>\n");
        return buf.toString();
    }

    /**
     * showBankUrlInfo - Bank URL Info Details
     */
    public String showBankSuppOrders(HttpSession session, HttpServletRequest request) {
    	Vector<OrderTypeMgr> vect = null;
    	Vector<OrdFormatMgr> vectFormat = null;
    	String hostId = null;
    	try {
    		initInfo(session, request);

    		hostId = request.getParameter("host");
    		vect = ProfileMgrDBProxy.getSupportedOrders(hostId);
    	} catch (Exception e) {}

    	StringBuffer buf = new StringBuffer();
    	String className = null;
    	OrderTypeMgr otmgr = null;
    	String transferType = null;
    	String ordDataType = null;
    	if (vect != null && vect.size() > 0) {
            Collections.sort(vect, new Comparator() {
                public int compare(Object o1, Object o2) {
                    OrderTypeMgr otm1 = (OrderTypeMgr) o1;
                    OrderTypeMgr otm2 = (OrderTypeMgr) o2;
                    String ot1 = otm1.getOrdType();
                    String ot2 = otm2.getOrdType();
                    return ot1.compareTo(ot2);
                }
            }); 
    		buf.append("<table border=0 cellpadding=0 cellspacing=0 valign='top' width=500>\n");
    		EBICSBeanBase e = new EBICSBeanBase();
    		EBICSBeanBase.Paginator p = e.new Paginator(request,pos,num,stepSize,19,new ArrayList(vect),langbean,"./Page?next=page.banksupptypes&host="+hostId,"Label.OrderType");
    		pos = p.getPos();
    		num = p.getNum();
    		stepSize = p.getStep();
    		List currentPageList = p.getCurrentPageList();
    		buf.append(p.getHeaderRow());
            addHSpace(buf, "40");
            buf.append( "<tr> ");
            buildTitle(buf, langbean.getValue("Label.OrderType"), 50);
            buildTitle(buf, langbean.getValue("Label.ProtocolVersion"), 50);
            buildTitle(buf, langbean.getValue("Label.ReleaseVersion"), 50);
            buildTitle(buf, langbean.getValue("Label.TransferType"), 50);
            buildTitle(buf, langbean.getValue("Label.OrderDataType"), 50);
            buildTitle(buf, langbean.getValue("Label.FileFormat") + "(" + langbean.getValue("Label.CountryCode") + ")", 0);
            addVSpace(buf, "tblBorder", "1", "1");
            buf.append( "</tr>\n");

            addHSpace(buf, "40");
            className = "grayEditSelection";
            for(int i = 0; i < currentPageList.size(); i++) {
                otmgr = (OrderTypeMgr)currentPageList.get(i);

    	   		if (i>0) {
    	   			if (className.equals("editSelection")) {
    	   				className = "grayEditSelection";
    	   			} else{
    	   				className = "editSelection";
    	   			}
    	   		}

	     		buf.append("<tr>");
	     		buildLine(buf,className,UIGlobals.processHTMLDisplayValue(otmgr.getOrdType()),"center");
	     		buildLine(buf,className,UIGlobals.processHTMLDisplayValue(otmgr.getProtocolVer()),"center");
	     		buildLine(buf,className,UIGlobals.processHTMLDisplayValue(otmgr.getReleaseVer()),"center");

	     		if (otmgr.getTransferType().equalsIgnoreCase("U")) transferType = "Upload";
	     		else if (otmgr.getTransferType().equalsIgnoreCase("D")) transferType = "Download";
	     		buildLine(buf,className,UIGlobals.processHTMLDisplayValue(transferType),"center");

	     		if (otmgr.getOrdDataType().equalsIgnoreCase("T")) ordDataType = "Technical";
	     		else if (otmgr.getOrdDataType().equalsIgnoreCase("S")) ordDataType = "System";
	     		buildLine(buf,className,UIGlobals.processHTMLDisplayValue(ordDataType),"center");

	     		vectFormat = otmgr.getOrdFormat();
	     		if (vectFormat.size() == 0) {
	     			buildLine(buf,className,"","left");
	     		} else {
                                Collections.sort(vectFormat, new Comparator() {
                                    public int compare(Object o1, Object o2) {
                                        OrdFormatMgr ofm1 = (OrdFormatMgr) o1;
                                        OrdFormatMgr ofm2 = (OrdFormatMgr) o2;
                                        String fm1 = ofm1.getFileFormat();
                                        String fm2 = ofm2.getFileFormat();
                                        if (!fm1.equals(fm2)) {
                                            return fm1.compareTo(fm2);
                                        } else {
                                            String cc1 = ofm1.getCountryCode();
                                            String cc2 = ofm2.getCountryCode();
                                            return cc1.compareTo(cc2); 
                                        }
                                    }
                                });
	     			// Inner Table
	     			addVSpace(buf);
	     			buf.append("<td class=" + className + ">");
	     			buf.append("<table border=0 cellpadding=0 cellspacing=0 valign='top'>\n");
	        		for (int j=0; j<vectFormat.size(); j++) {
		     			OrdFormatMgr format = vectFormat.get(j);
		        		buf.append( "<tr> ");
		        		String fileFormat = format.getFileFormat();
		        		if (format.getCountryCode() != null && !format.getCountryCode().trim().equals("")) {
		        			fileFormat += " (" + format.getCountryCode() + ")";
		        		}

			     		buildLine(buf,className,UIGlobals.processHTMLDisplayValue(fileFormat),"left");
		                buf.append("</tr>");
		     		}
	        		buf.append("</table>");
	   	     		buf.append("</td>");
	   	     		addVSpace(buf, className, "2", "1");
	     		}
	            addVSpace(buf, "tblBorder", "1", "1");
	            buf.append("</tr>\n");
	    	}
    		addHSpace(buf,"40");
    		buf.append(p.getFooterRow());
    	}
        buf.append("</table>\n");
        return buf.toString();
    }

	private void buildTitle(StringBuffer buf, String label, int width) {
		addVSpace(buf, "tblBorder", "1", "1");
		if (width == 0) {
			buf.append( "<td align='center' class=opsTblHdr>");
		} else {
			buf.append( "<td width=" + width + " align='center' class=opsTblHdr>");
		}
	    buf.append(label);
	    buf.append("</td>");
	    addVSpace(buf, "opsTblHdr", "2", "1");
	}

	private void buildTitle(StringBuffer buf, String label, int width, int colspan, int rowspan) {
		addVSpace(buf, "tblBorder", "1", "1");
		if (width == 0) {
			buf.append("<td colspan=" + colspan + " rowspan=" + rowspan + " align='center' class='opsTblHdr' nowrap>");
		} else {
			buf.append("<td width=" + width + " colspan=" + colspan + " rowspan=" + rowspan + " align='center' class='opsTblHdr' nowrap>");
		}
		buf.append(label);
	    buf.append("</td>");
	    addVSpace(buf, "opsTblHdr", "2", "1");
	}


    private void buildLine(StringBuffer buf,String className,String value,String align) {
    	addVSpace(buf);
 		buf.append("<td valign='top' align='" + align + "' class='");
        buf.append(className);
        buf.append("'>&nbsp;");
        if (value == null || value.trim().length() == 0)
 			buf.append (langbean.getValue("Label.notspecified"));
 		else
 			buf.append (value);
        buf.append("</td>");
        addVSpace(buf, className, "2", "1");
 	}

    private void addVSpace(StringBuffer sb, String className, String width, String height, int colspan, int rowspan) {
        sb.append("<td colspan=" + colspan + " rowspan=" + rowspan + " width='");

        if (width != null)
            sb.append(width);
        else
            sb.append("1");
        sb.append("' height='");
        if (height != null)
            sb.append(height);
        else
            sb.append("2");
        sb.append("' ");
        if (className != null) {
            sb.append("class='");
            sb.append(className);
            sb.append("'");
        }
        sb.append(">");
        makeImage(sb, "images.spacer", width, height, null);
        sb.append("</td>\n");

    }

    private void buildLine(StringBuffer buf, String className, String value, int width, int colspan, int rowspan) {
    	addVSpace(buf);
    	if (width == 0) {
    		buf.append("<td colspan=" + colspan + " rowspan=" + rowspan + " valign='top' align='center' class='");
    	} else {
    		buf.append("<td width=" + width + " colspan=" + colspan + " rowspan=" + rowspan + " valign='top' align='center' class='");
    	}
    	buf.append(className);
        buf.append("'>&nbsp;");
        if (value == null || value.trim().length() == 0)
 			buf.append (langbean.getValue("Label.notspecified"));
 		else
 			buf.append (value);
        buf.append("</td>");
        addVSpace(buf, className, "2", "1");
 	}

 // Related Utilities

    private void buildConfirmLine(StringBuffer buf, String label, String value) {
    	String className = "whiteStripe";
    	if (value == null || value.trim().length() == 0) {
    		value = langbean.getValue("Label.notspecified");
    	}

    	spacerLine(buf);
    	buf.append("<tr>");
    	startStripeCell(buf, className, "width='40%'");
    	buf.append(label);
    	buf.append("</td>\n");
    	addVSpace(buf, "ltspacer", "1", null);
    	addVSpace(buf, className, "5", null);
    	startStripeCell(buf, className, "width='60%'");
    	buf.append(value);
    	buf.append("</td>");
    	buf.append("</tr>\n");
    	spacerLine(buf);
    }

    public boolean search(HttpSession session,
    		HttpServletRequest request,
    		HttpServletResponse response){

		//QC#222262
		boolean returnDelete = false;

    	LangBean lang = (LangBean)session.getAttribute("langbean");
    	setLang(lang);

    	SessionInfo si = ((SessionInfo)session.getAttribute("SessionInfo"));
    	UserAutho authoObj = null ;
    	SessionData sd     = null ;
    	if( si != null ) {
    		authoObj = si.getAutho() ;
    		sd   = SessionDataContainer.instance().getSessData(session.getId() );
    	}
    	String order = null;
    	try {

    		if(editor == null)
    			editor = new BankProfileEditor(WizardObject.getWizardObjectType("BANKPROFILE"));

    		HostProfileMgr hostProfileMgr = editor.getHostProfileMgr();
    		if (hostProfileMgr == null)
    			hostProfileMgr = new HostProfileMgr();

			//QC#222262
			//Get 'returnDelete' parameter
			returnDelete = Boolean.parseBoolean(getRequestParam("returnDelete", request));

    		String dosearchFlag = getRequestParam("dosearch", request);
    		if(dosearchFlag == null)
    			dosearchFlag = request.getParameter("dosearch");

    		StringBuffer query = new StringBuffer();
    		String stringStart = getRequestParam("pos", request);

    		if(stringStart == null)
    			stringStart = request.getParameter("pos");

    		String stringNum = getRequestParam("num", request);

    		if(stringNum == null)
    			stringNum = request.getParameter("num");

    		if(stringStart != null)
    			pos = Integer.parseInt(stringStart);

    		if (stringNum != null)
    			num = Integer.parseInt(stringNum);

    		order = getRequestParam("order", request);
    		if(order == null)
    			order = request.getParameter("order");

    		if(order != null && !order.equals("")){
    			if(order.equals("ASC"))
    				asc = true;
    			else
    				asc = false;
    		}

    		int end = num;//No of records to retrieve

    		int index1 = 0;
    		bankProfileList = null;
    		resultList = null;
    		if(dosearchFlag == null || !dosearchFlag.equals("true")){
    			bankID = request.getParameter("bankID");

    			alphaSelect = getRequestParam("alpha", request);
    			if(alphaSelect == null)
    				alphaSelect =request.getParameter("alpha");
    		}

    		if(alphaSelect!= null)
    			index1 = alphaSelect.indexOf('[');

    		if(bankID != null && !bankID.equals("")) {
    			if( sd != null &&  authoObj!= null && authoObj.cacheSearchByValue())
    			{	sd.addValue(BankProfileEditor.CACHE_EBICS_BANK_ID, bankID);}

    			query.append(langbean.getValue("searchByName"));
    			query.append(": ");
    			query.append("<br>");
    			query.append("'");
    			query.append(UIGlobals.processHTMLDisplayValue(bankID));
    			query.append("'");
    			StringBuffer search = new StringBuffer();
                search.append("%");
                search.append(bankID.trim().toUpperCase());
                search.append("%");
    			totalCount = ProfileMgrDBProxy.getObjectCount(search.toString(), "EB_HOST");

    			if (pos + num > totalCount) {
    				end = totalCount - pos;
    			}

    			bankProfileList = null;
    			bankProfileList = ProfileMgrDBProxy.listByName(search.toString(),pos+1,end ,asc);
    		} else { // search Alphabetically
    			if(alphaSelect != null && !alphaSelect.equals("")){
    				if( sd != null &&  authoObj!= null && authoObj.cacheSearchByValue())
    					sd.addValue(BankProfileEditor.CACHE_EBICS_BANK_ALPHA, alphaSelect);

    				if(alphaSelect.equalsIgnoreCase("all")){
    					query.append(langbean.getValue("Label.BankProfile.ListAll"));
    					totalCount = ProfileMgrDBProxy.getObjectCount(null, "EB_HOST");
    					if (pos + num > totalCount){
    						end = totalCount - pos;
    					}
    					bankProfileList = ProfileMgrDBProxy.listByName(null,pos + 1,end,asc);

    				} else {
    					query.append(langbean.getValue("listAlpha"));
    					query.append(": ");
    					query.append("'");
    					query.append(alphaSelect.substring(index1+1, index1+2));
    					query.append("'");

    					totalCount = ProfileMgrDBProxy.getObjectCount(alphaSelect, "EB_HOST");
    					if (pos + num > totalCount)
    						end = totalCount - pos;
    					bankProfileList = ProfileMgrDBProxy.listByName(alphaSelect, pos + 1, end,asc);
    				}
    			}
    		}
    		if (query != null && query.length() > 0) {
    			displayquery = query.toString();
    		}

    		resultList = new ArrayList();
    		resultList = bankProfileList;

    		if(query != null && query.length() > 0)
    			displayquery = query.toString();

    		if(resultList == null || resultList.size() == 0) {
    			//QC#222262
    			if (!returnDelete) {
    				request.setAttribute("msg",
    					langbean.getValue("images.alertbutton") +
    					langbean.getValue("Label.BankProfile.NotFound"));
    			}
    			return false;
    		}
    	} catch(Exception ex) {
    		UIGlobals.out.logException("[BankProfileBean] search ", ex);
    		request.setAttribute("msg",
    				langbean.getValue("images.errorbutton") +
    				langbean.getValue("ProcessingError"));
    		return false;
    	}
    	return true;

    }

    // TODO  : check on the 2nd column alignment
    public String getList(HttpSession session,
    		HttpServletRequest request) {

    	LangBean lang = (LangBean) session.getAttribute("langbean");
    	setLang(lang);
    	setSession(session);
    	SessionInfo sessionInfo = (SessionInfo) session.getAttribute("SessionInfo");

    	// get if the Delete Permission is given.
        hasDeletePerm = (UIPermissions.checkPermission(sessionInfo.getAutho(), "EBICS_BANK_CONFIG_DEL") == UIPermissions.WRITE);

    	String stringStart = getRequestParam("pos", request);
    	if (stringStart == null) {
    		stringStart = request.getParameter("pos");
    	}
    	String stringNum = getRequestParam("num", request);
    	if (stringNum == null) {
    		stringNum = request.getParameter("num");
    	}

    	if (stringStart != null) {
    		pos = Integer.parseInt(stringStart);
    	}
    	if (stringNum != null){
    		num = Integer.parseInt(stringNum);
    	}

    	if (stepSize == -1){
    		stepSize = num;
    	}

    	int i, displaycnt = num;

    	if (pos + num > totalCount){
    		displaycnt = totalCount - pos;
    	}

    	String jumpbar = jumpbar(
    			"./Page?next=page.bankprofilelist&bad=page.bankconfigsearch&dosearch=true",
    			langbean.getValue("Label.BankProfile"), totalCount, stepSize,
    			pos);

    	StringBuffer s = new StringBuffer();
    	String className = "editSelection";

    	// Print "Bank Profile Configuration [start]-[start+num] of [total]"
    	s.append("<table cellpadding=0 cellspacing=0 border=0 width=550>\n");
    	s.append("<tr>\n");
    	s.append("<td class='searchNavHdr' align='left'>");
    	s.append(langbean.getValue("Title.BankProfiles"));
    	s.append(" ");
    	s.append(pos+1);
    	s.append("-");
    	s.append(pos+displaycnt);
    	s.append(" ");
    	s.append(langbean.getValue("of"));
    	s.append(" ");
    	s.append(totalCount);
    	// Print "Page: ..." -Jumpbar page nos.
    	s.append("</td>\n<td class='searchNavHdr' align='right'>\n");
    	s.append(jumpbar);
    	s.append("</td>\n</tr>\n</table>");
    	// Print the Search/List Result Table Header
    	s.append("<table cellpadding=0 cellspacing=0 border=0 width=550 valign='top' bgcolor='white'>\n");

    	addHSpace(s, "7");
    	s.append("<tr>\n");
    	// Prints "select"
    	addVSpace(s);

    	s.append("<td class='whiteTblHdr' align='center' noWrap width=100>\n");
    	s.append(lang.getValue("Label.Select"));
    	s.append("</td>\n");
    	// Prints "BankID"
    	addVSpace(s, "whiteTblHdr", "1","3");
    	s.append("<td class='whiteTblHdr' align='center' noWrap  width=150>");
    	s.append(langbean.getValue("Label.BankID"));
    	// Print "Sort Ascending"
    	s.append(" <a href=\"./Page?next=page.bankprofilelist&bad=page.bankconfigsearch&");
    	s.append("order=ASC");
    	s.append("&dosearch=true");
    	s.append("&pos=0&num=");
    	s.append(stepSize);
    	s.append("\" onMouseover=\"window.status='");
    	s.append(langbean.getValue("Label.sortAscending"));
    	s.append("'; return true;\"");
    	s.append(" onMouseout=\"window.status=''; ");
    	s.append("return true;\">");
    	makeImage(s, "images.sortup", null, null, null, "vspace=3");
    	s.append("</a>");
    	// Print "Sort Descending"
    	s.append(" <a href=\"./Page?next=page.bankprofilelist&bad=page.bankconfigsearch&");
    	s.append("order=DESC");
    	s.append("&dosearch=true");
    	s.append("&pos=0&num=");
    	s.append(stepSize);
    	s.append("\" onMouseover=\"window.status='");
    	s.append(langbean.getValue("Label.sortDescending"));
    	s.append("'; return true;\"");
    	s.append(" onMouseout=\"window.status=''; ");
    	s.append("return true;\">");
    	makeImage(s, "images.sortdown", null, null, null, "vspace=3");
    	s.append("</a>");
    	s.append("</td>");
    	addVSpace(s, "whiteTblHdr", "1","3");
    	s.append("<td class='whiteTblHdr' align='center' noWrap width=250>&nbsp;");
    	s.append(langbean.getValue("Label.BankName"));
    	s.append("</td>\n");
    	addVSpace(s);

    	addHSpace(s, "7");

    	addTblSpacer(s, new String[]{"1","1","1"}, "tblBorder", className, "3");

    	int loopcntr=0;
    	HashMap map = null;
    	for(i = 0; i<resultList.size(); i++) {
    		map = (HashMap) resultList.get(i);
    		// Get the bank id
    		id = (String)map.get(BankProfileEditor.BANK_ID);
    		displayname = (String)map.get(BankProfileEditor.BANK_NAME);

    		if (loopcntr > 0) { // don't switch color until one done
    			if (className.equals("editSelection"))
    				className = "grayEditSelection";
    			else
    				className = "editSelection";
    		}
    		loopcntr++;
    		s.append("<tr>");
    		addVSpace(s);

    		s.append("<td align='center' noWrap width=100 class='");
    		s.append(className);
    		// Edit
    		s.append("'><a href=\"./GetBankProfile?WizardAction=edit&bad=page.bankconfigsearch&");
    		s.append(uniqueKey());
    		s.append("&"+BankProfileEditor.BANK_ID);
    		s.append("=");
    		s.append(UIGlobals.processHTMLDisplayValue(id));
    		s.append("&"+BankProfileEditor.BANK_NAME+"=");
    		s.append(UIGlobals.processHTMLDisplayValue(displayname));
    		s.append("\" onMouseOver=\"window.status='");
    		s.append(langbean.getValue("Action.edit"));
    		s.append("'; return true;\" onMouseOut=\"");
    		s.append("window.status=''; return true;\"");
    		s.append(">");
    		makeImage(s, "images.edit", null, null, null, "align='absbottom'");
    		s.append("</a>");
    		//Delete
    		if (hasDeletePerm) {
    			s.append("<a href=\"./GetBankProfile?WizardAction=delete&bad=page.bankprofilelist&");
    			s.append(uniqueKey());
    			s.append("&dosearch=true");
    			s.append("&"+BankProfileEditor.BANK_ID);
    			s.append("=");
    			s.append(UIGlobals.processHTMLDisplayValue(id));
    			s.append("&"+BankProfileEditor.BANK_NAME+"=");
    			s.append(UIGlobals.processHTMLDisplayValue(displayname));
    			s.append("\" onMouseOver=\"window.status='");
    			s.append(langbean.getValue("Action.Delete"));
    			s.append("'; return true;\" onMouseOut=\"");
    			s.append("window.status=''; return true;\"");
    			s.append(" onClick=\"return confirmDeleteAll('");
    			s.append(Util.replaceString(
					UIGlobals.encodeJSString(
						langbean.getValue("Label.BankProfileConfig.DeleteWarning")),
    					"&NAME;", 
					UIGlobals.processHTMLDisplayValue(id))
				);
    			s.append("');\" ");
    			s.append(">");
    			makeImage(s, "images.delete", null, null, null, "align='absbottom'");
    			s.append("</a>");
    		}
    		s.append("</td>\n");
    		addVSpace(s);
    		s.append("<td width=150 class='");
    		s.append(className);
    		s.append("'>&nbsp;");
    		s.append("<a href=\"javascript:infoWin('./Page?next=page.bankProfileinfo&pos=0&num=15&");
    		s.append(uniqueKey());
    		s.append("&"+BankProfileEditor.BANK_ID+"=");
    		s.append(UIGlobals.processHTMLDisplayValue(id));
    		s.append("&"+BankProfileEditor.BANK_NAME+"=', '");
    		s.append(UIGlobals.processHTMLDisplayValue(displayname));
    		s.append("')");
    		s.append("\" onMouseover=\"window.status='");
    		s.append(UIGlobals.processHTMLDisplayValue(Util.replaceString(displayname,
    				"'", "\\'")));
    		s.append(" ");
    		s.append(langbean.getValue("Information"));
    		s.append("'; return true;\" ");
    		s.append("onMouseOut=\"window.status=''; return true;\" class='selectResource'>");
    		s.append(UIGlobals.processHTMLDisplayValue(id));
    		s.append("</a>");
    		s.append("</td>\n");
    		addVSpace(s);

    		s.append("<td width=150 class='");
    		s.append(className);
    		s.append("'>&nbsp;");
    		s.append(UIGlobals.processHTMLDisplayValue(displayname));
    		s.append("</td>\n");

    		addVSpace(s);
    		s.append("</tr>\n");
    		addTblSpacer(s, new String[] {"1","1","1"}, "tblBorder", className, "3");
    	}
    	addTblSpacer(s, new String[] {"1","1","1"}, "tblBorder", className, "3");
    	addHSpace(s, "7");

    	s.append("<tr><td class='searchNavHdr' colspan=7 align='right'>\n");
    	s.append(jumpbar);
    	s.append("</td>\n</tr>\n");

    	s.append("<tr>\n<td colspan=7 height=7 class='bodyspacer'>");
    	makeImage(s, "images.spacer", null, "5", null);
    	s.append("</td>\n</tr>\n");

    	s.append("<tr>\n");
    	s.append("<td height=35 align=right colspan=7 class='bodyspacer'>\n");
        String onClick = "window.location='./Page?next=page.bankconfigsearch&" + uniqueKey() + "'";
        new HTMLButtonBuilder().buildInputButton(s, "button", "Return", "wizard.Return", onClick, langbean, null);
    	s.append("</td>\n</tr>\n");
    	s.append("</table>\n");

    	return s.toString();
    }

    public String profileInfo(HttpSession session, HttpServletRequest request) {

    	StringBuffer buf = new StringBuffer();
    	String className = "grayStripe";
    	String val = null;
    	HostProfileMgr hpMgr = null;
    	Vector<OrderTypeMgr> supportedTypes = null;
    	LangBean langbean = (LangBean)session.getAttribute("langbean");
    	setLang(langbean);
    	String yesLabel = langbean.getValue("Yes");
    	String noLabel = langbean.getValue("No");

    	String bankid = request.getParameter(BankProfileEditor.BANK_ID);
    	UIGlobals.logDebug("Inside  the profile Info of BankProfileBean");
    	if(bankid != null){
    		try{
				bankid=bankid.trim();
    			supportedTypes = ProfileMgrDBProxy.getSupportedOrders(bankid);
    			hpMgr = ProfileMgrDBProxy.readHostMgr(bankid, true, true);
    		}
    		catch(Exception e){
    			UIGlobals.out.logException("[BankProfileBean] Exception when trying to load Bank Profile ", e);
    		}
    	}

    	buf.append("<table cellpadding=0 cellspacing=0 border=0 width=400 valign='top'>\n");
    	buf.append("<tr>");
    	startStripeCell(buf, "WizardInputText", "colspan=4");
    	buf.append(langbean.getValue("Label.BankProfileDetails"));
    	buf.append("</td>\n");
    	buf.append("</tr>\n");
    	addHSpace(buf, "4");
    	addHSpace(buf, "4", null, "1");
    	addHSpace(buf, "4");
    	spacerLine3(buf);

    	if (hpMgr == null) {
    		buf.append("<tr>");
    		startStripeCell(buf, className,"width=400");
    		buf.append(langbean.getValue("Label.noBankProfileDetails"));
    		buf.append("</td>\n");
    		buf.append("</tr>");

    		buf.append("</table>\n");

    		return buf.toString();
		}

    		addInfoLine(buf, langbean.getValue("Label.BankID"), UIGlobals.processHTMLDisplayValue(hpMgr.getHostID()));
    		addInfoLine(buf, langbean.getValue("Label.BankName"), UIGlobals.processHTMLDisplayValue(hpMgr.getFirstName()));
    		addInfoLine(buf, langbean.getValue("CP.Address1"), UIGlobals.processHTMLDisplayValue(hpMgr.getAddressLine1()));
    		addInfoLine(buf, langbean.getValue("CP.Address2"), UIGlobals.processHTMLDisplayValue(hpMgr.getAddressLine2()));
    		addInfoLine(buf, langbean.getValue("CP.City"), UIGlobals.processHTMLDisplayValue(hpMgr.getCity()));
    		addInfoLine(buf, langbean.getValue("CP.State"), UIGlobals.processHTMLDisplayValue(hpMgr.getState()));
    		String countryName = DigiCertBean.getCountries(langbean).getName(hpMgr.getCountry());
    		addInfoLine(buf, langbean.getValue("CP.Country"), UIGlobals.processHTMLDisplayValue(langbean.getValue(countryName)));    		
    		addInfoLine(buf, langbean.getValue("CP.Zip"), UIGlobals.processHTMLDisplayValue(hpMgr.getZipCode()));
    		String timezone = hpMgr.getTimeZone();
    		addInfoLine(buf, langbean.getValue("CP.TimeZone"), (timezone!= null && timezone.trim().length()>0) ? UIGlobals.processHTMLDisplayValue(langbean.getValue(EBICSConstants.EBICS_TAG+EBICSConstants.EBICS_TIMEZONE_DELIMITER+timezone)): null);
    		addInfoLine(buf, langbean.getValue("CP.EmailAddress"), UIGlobals.processHTMLDisplayValue(hpMgr.getEmailID()));
    		addInfoLine(buf, langbean.getValue("CP.PhoneNumber"), UIGlobals.processHTMLDisplayValue(hpMgr.getDayPhone()));

    		//addInfoLine(buf, langbean.getValue("Label.EncPriCert"), UIGlobals.processHTMLDisplayValue(sysCertList.getName(hpMgr.getEncrPrivKeyID())));
    		//addInfoLine(buf, langbean.getValue("Label.EncPubCert"), UIGlobals.processHTMLDisplayValue(trustedCertList.getName(hpMgr.getEncrPubKeyID())));
    		//addInfoLine(buf, langbean.getValue("Label.AuthPriCert"), UIGlobals.processHTMLDisplayValue(sysCertList.getName(hpMgr.getAuthPrivKeyID())));
    		//addInfoLine(buf, langbean.getValue("Label.AuthPubCert"), UIGlobals.processHTMLDisplayValue(trustedCertList.getName(hpMgr.getAuthPubKeyID())));
		String nonNISTCompliant = "&nbsp&nbsp<font color=\"red\">(" + langbean.getValue("error.notNISTCompliant", "Not NIST SP800-131a compliant") + ")</font>";
		addInfoLine(buf, langbean.getValue("Label.EncPriCert"), KeyUtil.getPrivateCertAlias(hpMgr.getEncrPrivKeyID(), nonNISTCompliant));
		addInfoLine(buf, langbean.getValue("Label.EncPubCert"), KeyUtil.getTrustedCertAlias(hpMgr.getEncrPubKeyID(), nonNISTCompliant));
    		addInfoLine(buf, langbean.getValue("Label.AuthPriCert"), KeyUtil.getPrivateCertAlias(hpMgr.getAuthPrivKeyID(), nonNISTCompliant));
    		addInfoLine(buf, langbean.getValue("Label.AuthPubCert"), KeyUtil.getTrustedCertAlias(hpMgr.getAuthPubKeyID(), nonNISTCompliant));
	
    		addInfoLine(buf, langbean.getValue("Label.MAXRecovery"), UIGlobals.processHTMLDisplayValue(Long.toString(hpMgr.getMaxRecoveryNum())));   		
    		addInfoLine(buf, langbean.getValue("Label.MaxSignAllowed"), UIGlobals.processHTMLDisplayValue(Long.toString(hpMgr.getMaxSigAllowed())));
    		addInfoLine(buf, langbean.getValue("Label.Prevalidation"), UIGlobals.processHTMLDisplayValue(hpMgr.getPrevalidate().trim().equals(EBICSConstants.DEF_SHORT_TRUE)? yesLabel : noLabel));
    		addInfoLine(buf, langbean.getValue("Label.SupportClientDownload"), UIGlobals.processHTMLDisplayValue(hpMgr.getClntDLSupport().trim().equals(EBICSConstants.DEF_SHORT_TRUE)? yesLabel : noLabel));
    		addInfoLine(buf, langbean.getValue("Label.SupportOrderDownload"), UIGlobals.processHTMLDisplayValue(hpMgr.getDlOrdSupport().trim().equals(EBICSConstants.DEF_SHORT_TRUE)? yesLabel : noLabel));
    		addInfoLine(buf, langbean.getValue("Label.SupportX509"), UIGlobals.processHTMLDisplayValue(hpMgr.getX509Support().trim().equals(EBICSConstants.DEF_SHORT_TRUE)? yesLabel : noLabel));
    		addInfoLine(buf, langbean.getValue("Label.PersistX509"), UIGlobals.processHTMLDisplayValue(hpMgr.getX509Persist().trim().equals(EBICSConstants.DEF_SHORT_TRUE)? yesLabel : noLabel));

    		StringBuffer bankUrlLink = new StringBuffer();
    		if (hpMgr.getHostURL().size() > 0) {
	        	bankUrlLink.append("<a class='selectResource' href=\"javascript:infoWin('./Page?");
	        	bankUrlLink.append(uniqueKey());
	        	bankUrlLink.append("&next=page.bankurlinfo&pos=0&num=15&isConfirmPage=false&host=" + bankid + "&" + WizardBase.WIZARD_TYPE + "=BankProfile&" + WizardBase.WIZARD_OBJECT_TYPE + "=" + WizardObject.getWizardObjectType("BANKPROFILE"));
	        	bankUrlLink.append("', '')\">");
	        	bankUrlLink.append(langbean.getValue("Label.ShowAll"));
	            bankUrlLink.append("</a>\n");
    		}
    		addInfoLine(buf, langbean.getValue("Label.BankURLConfig"), bankUrlLink.toString());

    		StringBuffer bankProtLink = new StringBuffer();
    		if (hpMgr.getHostProtocol().size() > 0) {
    			bankProtLink.append("<a class='selectResource' href=\"javascript:infoWin('./Page?");
    			bankProtLink.append(uniqueKey());
    			bankProtLink.append("&next=page.bankprotocolinfo&pos=0&num=15&isConfirmPage=false&host=" + bankid + "&" + WizardBase.WIZARD_TYPE + "=BankProfile&" + WizardBase.WIZARD_OBJECT_TYPE + "=" + WizardObject.getWizardObjectType("BANKPROFILE"));
    			bankProtLink.append("', '')\">");
    			bankProtLink.append(langbean.getValue("Label.ShowAll"));
    			bankProtLink.append("</a>\n");
    		}
    		addInfoLine(buf, langbean.getValue("Label.BankProtocolConfig") + "/" + langbean.getValue("Label.BankProcessConfig"), bankProtLink.toString());

    		StringBuffer bankSuppLink = new StringBuffer();
    		if (supportedTypes.size() > 0) {
    			bankSuppLink.append("<a class='selectResource' href=\"javascript:infoWin('./Page?");
    			bankSuppLink.append(uniqueKey());
    			bankSuppLink.append("&next=page.banksupptypes&pos=0&num=15&isConfirmPage=false&host=" + bankid + "&" + WizardBase.WIZARD_TYPE + "=BankProfile&" + WizardBase.WIZARD_OBJECT_TYPE + "=" + WizardObject.getWizardObjectType("BANKPROFILE"));
    			bankSuppLink.append("', '')\">");
    			bankSuppLink.append(langbean.getValue("Label.ShowAll"));
    			bankSuppLink.append("</a>\n");
    		}
    		addInfoLine(buf, langbean.getValue("Label.BankSupported"), bankSuppLink.toString());

    		spacerLine3(buf);
    		addHSpace(buf, "4");
    		addHSpace(buf, "4", null, "20");
    		buf.append("</table>\n");

    	return buf.toString();
    }

    private void addInfoLine(StringBuffer buf, String label, String value) {
    	String className = "grayStripe";
    	if (value == null || value.trim().length() == 0) {
    		value = langbean.getValue("Label.notspecified");
    	}

		buf.append("<tr>");
		startStripeCell(buf, className,"width=200");
		buf.append(label);
		buf.append("</td>\n");
		addVSpace(buf, "whitespacer", "1", null);
		addVSpace(buf, className, "5", null);
		startStripeCell(buf, className, "width=200");
		buf.append(value);
		buf.append("</td>");
		buf.append("</tr>\n");

		spacerLine3(buf);
		addHSpace(buf, "4", "whitespacer","1");
		spacerLine3(buf);
    }

    public String buildSysCertListDisplay(String Certfilter, String certField){

    	StringBuffer s = new StringBuffer();
    	s.append("");
    	NameValuePairs CertList = new NameValuePairs();;
    	if(certField.trim().equalsIgnoreCase(editor.EncPriCert)|| certField.trim().equalsIgnoreCase(editor.AuthPriCert))
    	{
    		CertList = sysCertList;
    	}else if(certField.trim().equalsIgnoreCase(editor.EncPubCert)|| certField.trim().equalsIgnoreCase(editor.AuthPubCert)){
    		CertList = trustedCertList;
    	}

    	NameValuePairs _sts = new NameValuePairs();
    	NameValue _st ;
    	if( Certfilter == null || Certfilter.trim().length() == 0) {
    		_sts = CertList ;
    		Certfilter= "";

    	} else {
    		for (int k=0; k < CertList.size(); k++) {
    			_st = CertList.getElement(k) ;

    			if (((String) _st.getName()).toLowerCase().indexOf(Certfilter.toLowerCase()) != -1 ) {
    				_sts.addElement(_st);

    			}
    		}

    	}
    	_sts.sortStringValue(false);

    	boolean usingNS = false ;
    	int i_ns = -1;
    	if( wiz.session() != null )
    		i_ns = ((Integer)wiz.session().getAttribute("NS4Browser")).intValue();
    	if( i_ns == 1 ) usingNS = true;
    	buildSinglePicker( s , _sts, true, 500 ,langbean.getValue( "Filter" ) , langbean.getValue( "bytype" ), "Certfilter", Certfilter , langbean.getValue("Label.Select"), "" , "sysCerttype" , "", false , null, false, 35, 300, 200, 85, usingNS);

    	return s.toString();
    }

}
