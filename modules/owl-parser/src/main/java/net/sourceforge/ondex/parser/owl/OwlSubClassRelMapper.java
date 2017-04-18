package net.sourceforge.ondex.parser.owl;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.util.iterator.ExtendedIterator;

import net.sourceforge.ondex.core.EvidenceType;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.core.RelationType;
import net.sourceforge.ondex.core.utils.CachedGraphWrapper;
import net.sourceforge.ondex.parser.RelationsMapper;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Apr 2017</dd></dl>
 *
 */
public class OwlSubClassRelMapper implements RelationsMapper<OntModel>
{
	private OWLConceptClassMapper conceptClassMapper;
	
	private OWLConceptMapper conceptMapper;	
	
	public Stream<ONDEXRelation> map ( OntModel model, ONDEXGraph graph ) 
	{
		OWLConceptMapper conceptMapper = this.getConceptMapper ();
		if ( conceptMapper.getConceptClassMapper () == null ) 
			conceptMapper.setConceptClassMapper ( conceptClassMapper );
		
		// The top ontology class is a concept class, ontology subclasses are instances of the top class, 
		// all other descendants are instances too, plus they have subclass relations, but they don't have the suclass
		// relation to the top class (which isn't even a concept)
		//
		OntClass topOntCls = model.getOntClass ( this.getConceptClassMapper ().getClassIri () );

		return
			StreamSupport.stream (
				Spliterators
				.spliteratorUnknownSize ( topOntCls.listSubClasses ( true ), Spliterator.IMMUTABLE ),
				true
			)
			.flatMap ( ontChild -> this.map ( ontChild, graph ) );
	}
	
	protected Stream<ONDEXRelation> map ( OntClass rootCls, ONDEXGraph graph )
	{
		OWLConceptMapper conceptMapper = this.getConceptMapper ();
		RelationType relType = this.getRelationType ( graph );
		EvidenceType evidence = this.getEvidenceType ( graph );
		
		ONDEXConcept rootConcept = conceptMapper.map ( rootCls, graph );
		
		// First get all the links at this hierarchical level
		Stream<ONDEXRelation> result =
			StreamSupport.stream (
				Spliterators
				.spliteratorUnknownSize ( rootCls.listSubClasses ( true ), Spliterator.IMMUTABLE ),
				true
			)
			.map ( child -> CachedGraphWrapper.getInstance ( graph ).getRelation ( 
				conceptMapper.map ( child, graph ), rootConcept, relType, evidence 
			));
		
		// Next, recurse and add up more 
		for ( 
			ExtendedIterator<OntClass> children = rootCls.listSubClasses ( true ); 
			children.hasNext ();
			result = Stream.concat ( result, this.map ( children.next (), graph ) )
		);
		return result;
	}
	
	
	public RelationType getRelationType ( ONDEXGraph graph )
	{
		return CachedGraphWrapper.getInstance ( graph ).getRelationType ( 
			"is_a", 
			true, // isAntisymmetric, 
			true, // isReflexive, 
			false, // isSymmetric, 
			true // isTransitive 
		);
	}

	public EvidenceType getEvidenceType ( ONDEXGraph graph )
	{
		return CachedGraphWrapper.getInstance ( graph ).getEvidenceType ( "IMPD", "IMPD", "" );
	}

	
	public OWLConceptClassMapper getConceptClassMapper ()
	{
		return this.conceptClassMapper;
	}

	public void setConceptClassMapper ( OWLConceptClassMapper conceptClassMapper )
	{
		this.conceptClassMapper = conceptClassMapper;
	}

	public OWLConceptMapper getConceptMapper ()
	{
		return conceptMapper;
	}

	public void setConceptMapper ( OWLConceptMapper conceptMapper )
	{
		this.conceptMapper = conceptMapper;
	}
}
