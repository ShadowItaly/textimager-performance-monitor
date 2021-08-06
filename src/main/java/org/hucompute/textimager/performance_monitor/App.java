package org.hucompute.textimager.performance_monitor;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.languagetool.LanguageToolSegmenter;
import org.hucompute.textimager.uima.steps.StepsParser;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws UIMAException
    {
        System.out.println( "Hello World!" );
        JCas jCas = JCasFactory.createText("This is a simple test sentence to test this tool. Using a second sentence.",
                "en");
        System.out.println( "Still alive" );

        AnalysisEngineDescription segmenter = createEngineDescription(LanguageToolSegmenter.class);

        AnalysisEngineDescription stepsParser = createEngineDescription(StepsParser.class,
                StepsParser.PARAM_REST_ENDPOINT, "http://localhost:8000"
        );

        StepsParser.set_batch_size(jCas,22);
        SimplePipeline.runPipeline(jCas, segmenter, stepsParser);

        JCasUtil.select(jCas, Dependency.class).stream().forEach(System.out::println);
    }
}
