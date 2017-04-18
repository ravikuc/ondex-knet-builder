package net.sourceforge.ondex.parser.owl;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.jena.ontology.OntClass;

import net.sourceforge.ondex.parser.SimpleIdMapper;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>6 Apr 2017</dd></dl>
 *
 */
public class IRIBasedIdMapper implements SimpleIdMapper<OntClass>
{
	private String splitRegEx = "[#/]";
	
	@Override
	public String map ( OntClass ontCls )
	{
		// TODO: null
		String iri= ontCls.getURI (); 
		try
		{
			String[] frags = iri.split ( splitRegEx );
			String lastFrag = frags [ frags.length - 1 ];
			lastFrag = URLDecoder.decode ( lastFrag, "UTF-8" );
			lastFrag = lastFrag.replaceAll ( "[\\s+-:.?]", "_" );
			return lastFrag;
		}
		catch ( UnsupportedEncodingException ex ) {
			throw new RuntimeException ( String.format ( 
				"Internal error while extracting an ID from the uri <%s>: %s", iri, ex.getMessage ()
			), ex );
		}
	}


	public String getSplitRegEx ()
	{
		return splitRegEx;
	}


	public void setSplitRegEx ( String splitRegEx )
	{
		this.splitRegEx = splitRegEx;
	}
	
}
