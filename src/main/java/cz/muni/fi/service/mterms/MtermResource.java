/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.service.mterms;

import cz.muni.fi.mias.math.Formula;
import cz.muni.fi.mias.math.MathMLConf;
import cz.muni.fi.mias.math.MathTokenizer;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Service providing MathML to M-terms and M-term to MathML conversion
 *
 * @author Martin Liska
 */
@Path("/mterms")
public class MtermResource {

    private Map<String,String> eldict;
    private Map<String,String> reveldict = new HashMap<String,String>();

    
    /** Creates a new instance of MathProcess */
    public MtermResource() {
        eldict = new MathMLConf().getElementDictionary();
        for (Map.Entry<String,String> entry : eldict.entrySet()) {
            reveldict.put(entry.getValue(), entry.getKey());
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public MIaSTermContainer convertMathML(@FormParam("mathml") String query) throws IOException {
        return process(query);
    }

    private MIaSTermContainer process(String query) throws IOException {
        System.out.println(query);
        List<MIaSTerm> result = new ArrayList<MIaSTerm>();
        String mathQuery = "<html>" + query + "</html>";
        MathTokenizer mt = new MathTokenizer(new StringReader(mathQuery), true, MathTokenizer.MathMLType.BOTH);
//        mt.reset(new StringReader(mathQuery), prefix);
        Map<Integer, List<Formula>> forms = mt.getFormulae();
        for (int i = 0; i < forms.size(); i++) {
            List<Formula> flist = forms.get(i);
            for (int j = 0; j < flist.size(); j++) {
                Formula f = flist.get(j);
                result.add(new MIaSTerm(Formula.nodeToString(f.getNode(), false, MathMLConf.getElementDictionary(), MathMLConf.getAttrDictionary(), MathMLConf.getIgnoreNode()), f.getWeight()));
            }
        }
        MIaSTermContainer fc = new MIaSTermContainer();
        fc.setForms(result);
        return fc;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("reverse")
    public MathMLTerm convertMterm(@FormParam("mterm") String query) throws IOException {
        String result = reverse(query);
        result = addIdsConsts(result);
        if (!result.startsWith("<math>")) {
            result = "<math>" + result + "</math>";
        }
        MathMLTerm mmlterm = new MathMLTerm(result);
        return mmlterm;
    }

    private String reverse(String s) {
        String result = "";
//        System.out.println(s);
        if (s.length()>1 && (s.charAt(1)=='(' || s.charAt(1)=='[')) {
            String attr = "";
            boolean hasattr = false;
            if (s.charAt(1)=='[') {
                hasattr = true;
                attr = " " + reveldict.get(String.valueOf(s.charAt(2))) + "=\"" +reveldict.get(String.valueOf(s.charAt(4))) +"\"";
            }
            String tag = reveldict.get(String.valueOf(s.charAt(0)));
            int bracket = getLastBracketIndex(s);
            result += "<" + tag + attr + ">" + reverse(s.substring(hasattr?7:2,bracket)) + "</" + tag + ">" + reverse(s.substring(bracket+1));
        } else {
            result += s;
        }
        return result;
    }

    private String addIdsConsts(String s) {
        StringBuilder sb = new StringBuilder(s);
        int i1 = 0;
        int i2 = 0;
        String start = "<mi>";
        String end = "</mi>";
        while ((i1 = sb.indexOf(start,i1)) != -1) {
            i1 += start.length();
            i2 = sb.indexOf(end, i1 +1);
            String text = sb.substring(i1, i2);
            if (text.matches("(?=[^A-Za-z]+$).*[0-9].*")) {
                text = "id"+text;
            }
            sb.replace(i1, i2, text);
        }
        while ((i1 = sb.indexOf("¶",i1+1)) != -1) {
            sb.replace(i1, i1+1, "const");
        }
        return sb.toString();
    }

    private int getLastBracketIndex(String s) {
        int count = 0;
        boolean can = false;
        for (int i = 0; i<s.length(); i++) {
            if (s.charAt(i)=='(') {
                count++;
                can = true;
            }
            if (s.charAt(i)==')') count--;
            if (count==0 && can) return i;
        }
        return 0;
    }
}
