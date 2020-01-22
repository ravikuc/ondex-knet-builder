package net.sourceforge.ondex.rdf.export;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Model;

import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.ONDEXGraphMetaData;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.rdf.export.mappers.RDFXFactory;
import net.sourceforge.ondex.rdf.export.util.RDFXFactoryBatchCollector;
import uk.ac.ebi.utils.threading.batchproc.ItemizedBatchProcessor;

/**
 * The RDFExporter machinery. 
 * 
 * This triggers the exporting of an {@link ONDEXGraph}, passing it to the {@link RDFXFactory java2rdf-based RDF mappers}
 * and calling an {@link #setBatchJob(java.util.function.Consumer) handler} for the RDF chunks generated during the process.
 * 
 * The handler is supposed to do some concrete job, such as saving to a file or sending the RDF to a
 * triple store. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>21 Nov 2017</dd></dl>
 *
 */
public class RDFExporter 
	extends ItemizedBatchProcessor<Object, RDFXFactory, RDFXFactoryBatchCollector, Consumer<RDFXFactory>>
{
	/** Keep track of all the exported triples, for logging purposes. */
	private long triplesCount = 0;
	
	
	public RDFExporter () {
		this ( null );
	}

	public RDFExporter ( Consumer<RDFXFactory> batchJob ) {
		super ( batchJob, new RDFXFactoryBatchCollector () );
	}


	public void export ( ONDEXGraph graph )
	{
		log.info ( "Exporting graph meta-entities" );

		// We export all metadata in one chunk. This is typically small at this point and flushing it out 
		// allows a client to handle the whole T-Box first.
		
		// TODO: Graph summaries too
		RDFXFactoryBatchCollector batchColl = this.getBatchCollector ();
		RDFXFactory xfactBatch = batchColl.batchFactory ().get ();
		BiConsumer<RDFXFactory, Object> accumulator = batchColl.accumulator ();
		ONDEXGraphMetaData metaData = graph.getMetaData ();
		Stream.of ( 
			metaData.getConceptClasses (), 
			metaData.getRelationTypes (), 
			metaData.getAttributeNames (), 
			metaData.getEvidenceTypes (),
			metaData.getUnits ()
		)
		.flatMap ( Collection::stream )
		.forEach ( meta -> accumulator.accept ( xfactBatch, (Object) meta ) );
		
		this.handleNewBatch ( xfactBatch, true );

		
		// And now the rest, following the usual facilities from the BatchProcessor class.
		//
		
		Set<ONDEXConcept> concepts = graph.getConcepts ();
		log.info ( "Exporting {} concept(s)", concepts.size () );
		super.process ( concepts.stream()::forEach );
		

		Set<ONDEXRelation> relations = graph.getRelations ();
		log.info ( "Exporting {} relation(s)", relations.size () );
		super.process ( relations.stream() ::forEach );

		log.info ( 
			"RDF export finished, a total of {} concepts+relations exported, {} triples created",
			concepts.size () + relations.size (), triplesCount
		);
	}
	

	/**
	 * Wraps the model generated by {@link #getBatchFactory()} into a a new {@link RDFXFactory}, which 
	 * is the destination to which {@link #export(ONDEXGraph)} sends mappings instructions.
	 *  
	 */
	@Override
	protected RDFXFactory handleNewBatch ( RDFXFactory currentBatch, boolean forceFlush )
	{
		RDFXFactory newBatch = super.handleNewBatch ( currentBatch, forceFlush );
		if ( newBatch == currentBatch ) return currentBatch;
		
		Model currentModel = currentBatch.getJenaModel ();
		triplesCount += currentModel.size ();
		log.debug ( "{} RDF triples submitted for export", triplesCount );
		
		return newBatch;
	}
			
}
