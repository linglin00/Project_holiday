package WebScraper;

import org.apache.lucene.analysis.Analyzer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.mahout.clustering.Cluster;
import org.apache.mahout.clustering.canopy.CanopyDriver;
import org.apache.mahout.clustering.classify.WeightedVectorWritable;
import org.apache.mahout.clustering.evaluation.ClusterEvaluator;
import org.apache.mahout.clustering.kmeans.KMeansDriver;
import org.apache.mahout.clustering.kmeans.RandomSeedGenerator;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.commandline.DefaultOptionCreator;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.common.iterator.sequencefile.PathFilters;
import org.apache.mahout.common.iterator.sequencefile.PathType;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileDirIterable;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.utils.clustering.ClusterDumper;
import org.apache.mahout.utils.vectors.VectorHelper;
import org.apache.mahout.vectorizer.DictionaryVectorizer;
import org.apache.mahout.vectorizer.DocumentProcessor;
import org.apache.mahout.vectorizer.common.PartialVectorMerger;
import org.apache.mahout.vectorizer.tfidf.TFIDFConverter;
import java.lang.reflect.Field;

public class DocTokenizer {
	private static MyAnalyzer analyzer = new MyAnalyzer();

	public static void main(String[] argv)  throws Exception {
		Configuration conf = new Configuration();
		Path inputDir = new Path("/tmp/travelblog/North-America-seqdir");
		Path tokenizedDocumentsPath = new Path("/tmp/travelblog/North-America-seqdir-sparse-kmeans/tokenized-documents");
		Path outputDir = new Path("/tmp/travelblog/North-America-seqdir-sparse-kmeans/");
		//Path tfidfPath = new Path("/tmp/travelblog/North-America-seqdir-sparse-kmeans/tfidf-vectors");
		int minSupport = 1;
		int minDf = 2;
		int maxDFPercent = 85;
		int maxNGramSize = 2;
		int minLLRValue = 50;
		int reduceTasks = 1;
		int chunkSize = 64;
		int norm = 2;
		boolean sequentialAccessOutput = true;
		
		DocumentProcessor.tokenizeDocuments(inputDir, analyzer.getClass().asSubclass(Analyzer.class), tokenizedDocumentsPath, conf);
		DictionaryVectorizer.createTermFrequencyVectors(tokenizedDocumentsPath, outputDir, DictionaryVectorizer.DOCUMENT_VECTOR_OUTPUT_FOLDER,
				conf, minSupport, maxNGramSize, minLLRValue, 2, true, reduceTasks, chunkSize, sequentialAccessOutput, false);
		Pair<Long[], List<Path>> documentFrequencies = TFIDFConverter.calculateDF(new Path(outputDir, DictionaryVectorizer.DOCUMENT_VECTOR_OUTPUT_FOLDER), outputDir, conf, 100);
		TFIDFConverter.processTfIdf(new Path(outputDir, DictionaryVectorizer.DOCUMENT_VECTOR_OUTPUT_FOLDER), outputDir, 
				conf, documentFrequencies, minDf, maxDFPercent, norm, true, sequentialAccessOutput, false, reduceTasks);
		
		
		
		double convergenceDelta = 0.001;
		int numClusters = 5;
		int maxIterations = 10;
		DistanceMeasure measure = new EuclideanDistanceMeasure();
		Path tfidfVectorPath = new Path(outputDir, "tfidf-vectors");
		//Path canopyCentroidPath = new Path(outputDir ,"canopy-centroids");
				
		Path clusterOutput = new Path(outputDir , "clusters");
	   	Path clustersIn = new Path(outputDir, "random-seeds");
    	RandomSeedGenerator.buildRandom(conf, tfidfVectorPath, clustersIn, numClusters, measure);
    	KMeansDriver.run(tfidfVectorPath, clustersIn, clusterOutput, convergenceDelta, maxIterations, true, 0.0, true);
    	ClusterEvaluator cv = new ClusterEvaluator(conf,new Path(clusterOutput,"clusters-3-final"));

    	System.out.println(cv.interClusterDensity());

    	System.out.println(cv.intraClusterDensity());
        //List<String> results = new ArrayList<String>();

        /*Path pointsPath = new Path(clusterOutput, "clusteredPoints/part-m-0");
        for (Pair<IntWritable, WeightedVectorWritable> record : new SequenceFileDirIterable<IntWritable, WeightedVectorWritable>(
                pointsPath, PathType.GLOB, PathFilters.logsCRCFilter(), conf)) {
            NamedVector vec = ((NamedVector) record.getSecond().getVector());
            System.out.println(record.getFirst().get() + "  " + vec.getName());
        }*/
       
	   	final Path pointsDir = new Path(clusterOutput, "clusteredPoints/part-m-0");
        final ClusterDumper clusterDumper = new ClusterDumper(finalClusterPath(
                conf, clusterOutput, numClusters), pointsDir);
        String dictFile = outputDir.toString()+ "/dictionary.file-*";
        /*final Class<? extends ClusterDumper> clazz = clusterDumper.getClass();
        final Field field = clazz.getSuperclass().getDeclaredField("outputFile");
        field.setAccessible(true);
        field.set(clusterDumper, new File(outputDir.toString() + "/clusterdump"));
        
        final String[] dictionary = VectorHelper.loadTermDictionary(conf, dictFile);
        clusterDumper.printClusters(dictionary);*/
        
        
        /*clusterDumper.run(new String[] {
                buildOption(DefaultOptionCreator.INPUT_OPTION),
                clusterOutput.toString(),
                buildOption(DefaultOptionCreator.OUTPUT_OPTION),
                outputDir.toString() + "/clusterdump", 
                buildOption(ClusterDumper.OUTPUT_FORMAT_OPT),
                ClusterDumper.OUTPUT_FORMAT.TEXT.toString(),
                buildOption(ClusterDumper.POINTS_DIR_OPTION),
                pointsDir.toString(),
                buildOption(ClusterDumper.DICTIONARY_OPTION),
                dictFile,
                buildOption(ClusterDumper.NUM_WORDS_OPTION), "10",
                // buildOption(ClusterDumper.SAMPLE_POINTS), "30",
                buildOption(ClusterDumper.DICTIONARY_TYPE_OPTION),
                "sequencefile", buildOption(ClusterDumper.EVALUATE_CLUSTERS) });*/
	}
	
	private static final String buildOption(final String option) {
        return String.format("--%s", option);
	}
	
	private static Path finalClusterPath(Configuration conf, Path output, 
	      int maxIterations) throws IOException { 
	    FileSystem fs = FileSystem.get(conf); 
	    for (int i = maxIterations; i >= 0; i--) { 
	      Path clusters = new Path(output, "clusters-" + i + "-final"); 
	      if (fs.exists(clusters)) { 
	        return clusters; 
	      } 
	    } 
	    return null; 
	} 
}
