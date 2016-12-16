package WebScraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.lucene.analysis.Analyzer;
import org.apache.mahout.clustering.AbstractCluster;
import org.apache.mahout.clustering.Cluster;
import org.apache.mahout.clustering.canopy.CanopyDriver;
import org.apache.mahout.clustering.classify.ClusterClassifier;
import org.apache.mahout.clustering.classify.WeightedVectorWritable;
import org.apache.mahout.clustering.display.DisplayKMeans;
import org.apache.mahout.clustering.iterator.ClusterIterator;
import org.apache.mahout.clustering.iterator.ClusterWritable;
import org.apache.mahout.clustering.iterator.KMeansClusteringPolicy;
import org.apache.mahout.clustering.kmeans.KMeansDriver;
import org.apache.mahout.clustering.kmeans.RandomSeedGenerator;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.common.distance.ManhattanDistanceMeasure;
import org.apache.mahout.common.iterator.sequencefile.PathFilters;
import org.apache.mahout.common.iterator.sequencefile.PathType;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileDirValueIterable;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.vectorizer.DictionaryVectorizer;
import org.apache.mahout.vectorizer.DocumentProcessor;
import org.apache.mahout.vectorizer.common.PartialVectorMerger;
import org.apache.mahout.vectorizer.tfidf.TFIDFConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class KMean {
	private static Configuration conf = new Configuration();
	private static MyAnalyzer analyzer = new MyAnalyzer();
	private static Logger logger = LoggerFactory.getLogger(KMean.class);
	private static final List<VectorWritable> SAMPLE_DATA = new ArrayList<>();
	
	public static void main(String[] args) throws Exception {
		DistanceMeasure measure = new EuclideanDistanceMeasure();
		Path samples = new Path("/tmp/mahout-work-ling/reuters-out");
		Path output = new Path("/tmp/mahout-output");
		Configuration conf = new Configuration();
		HadoopUtil.delete(conf, samples);
		HadoopUtil.delete(conf, output);
		    
		RandomUtils.useTestSeed();
		boolean runClusterer = true;
		double convergenceDelta = 0.001;
		int numClusters = 20;
		int maxIterations = 10;
		if (runClusterer) {
			runSequentialKMeansClusterer(conf, samples, output, measure, numClusters, maxIterations, convergenceDelta);
		} else {
		    runSequentialKMeansClassifier(conf, samples, output, measure, numClusters, maxIterations, convergenceDelta);
		}
	}
		  
    private static void runSequentialKMeansClassifier(Configuration conf, Path samples, Path output,
    												  DistanceMeasure measure, int numClusters, int maxIterations, double convergenceDelta) throws IOException {
    	Collection<Vector> points = Lists.newArrayList();
    	for (int i = 0; i < numClusters; i++) {
    		points.add(SAMPLE_DATA.get(i).get());
    	}
    	List<Cluster> initialClusters = Lists.newArrayList();
    	int id = 0;
    	for (Vector point : points) {
    		initialClusters.add(new org.apache.mahout.clustering.kmeans.Kluster(point, id++, measure));
    	}
    	ClusterClassifier prior = new ClusterClassifier(initialClusters, new KMeansClusteringPolicy(convergenceDelta));
    	Path priorPath = new Path(output, Cluster.INITIAL_CLUSTERS_DIR);
    	prior.writeToSeqFiles(priorPath);
    
    	ClusterIterator.iterateSeq(conf, samples, priorPath, output, maxIterations);
    	loadClustersWritable(output);
    }
  
    private static void runSequentialKMeansClusterer(Configuration conf, Path samples, Path output, 
    												 DistanceMeasure measure, int numClusters, int maxIterations, double convergenceDelta)
    												 throws IOException, InterruptedException, ClassNotFoundException {
    	Path clustersIn = new Path(output, "random-seeds");
    	RandomSeedGenerator.buildRandom(conf, samples, clustersIn, numClusters, measure);
    	KMeansDriver.run(samples, clustersIn, output, convergenceDelta, maxIterations, true, 0.0, true);
    	loadClustersWritable(output);
    }
  
    protected static List<Cluster> readClustersWritable(Path clustersIn) {
	    List<Cluster> clusters = new ArrayList<>();
	    Configuration conf = new Configuration();
	    for (ClusterWritable value : new SequenceFileDirValueIterable<ClusterWritable>(clustersIn, PathType.LIST, PathFilters.logsCRCFilter(), conf)) {
	        Cluster cluster = value.getValue();
	        logger.info(
	          "Reading Cluster:{} center:{} numPoints:{} radius:{}",
	          cluster.getId(), AbstractCluster.formatVector(cluster.getCenter(), null),
	          cluster.getNumObservations(), AbstractCluster.formatVector(cluster.getRadius(), null));
	        clusters.add(cluster);
	    }
	    return clusters;  
    }
	
    protected static void loadClustersWritable(Path output) throws IOException {
	    Configuration conf = new Configuration();
	    FileSystem fs = FileSystem.get(output.toUri(), conf);
	    /*for (FileStatus s : fs.listStatus(output, new ClustersFilter())) {
	       List<Cluster> clusters = readClustersWritable(s.getPath());
	       CLUSTERS.add(clusters);
	    }*/
	}
    
    
	private static void calculateTfIdf(String outputDir, Path documentsSequencePath, Path tokenizedDocumentsPath, Path termFrequencyVectorsPath, Path tfidfPath) throws ClassNotFoundException, IOException,InterruptedException {
		int minSupport = 2;
		int minDf = 5;
		int maxDFPercent = 95;
		int maxNGramSize = 2;
		int minLLRValue = 50;
		int reduceTasks = 1;
		int chunkSize = 200;
		int norm = 2;
		boolean sequentialAccessOutput = true;
		
		DocumentProcessor.tokenizeDocuments(documentsSequencePath, analyzer.getClass().asSubclass(Analyzer.class), tokenizedDocumentsPath, conf);

		DictionaryVectorizer.createTermFrequencyVectors(tokenizedDocumentsPath, termFrequencyVectorsPath, "my-vector", conf, 1, 1, 0.0f, PartialVectorMerger.NO_NORMALIZING, true, 1, 100, false, true);

		Pair<Long[], List<Path>> documentFrequencies = TFIDFConverter.calculateDF(termFrequencyVectorsPath, tfidfPath, conf, 100);

		TFIDFConverter.processTfIdf(termFrequencyVectorsPath, tfidfPath, conf, documentFrequencies, 1, 100, PartialVectorMerger.NO_NORMALIZING, false, sequentialAccessOutput, false, reduceTasks);
	}

	/*public static void main(String args[]) throws Exception {
		
		String inputDir = "/tmp/mahout-work-ling";
		
		FileSystem fs = FileSystem.get(conf);
		String outputDir = "/tmp/mahout-out";
		//HadoopUtil.delete(conf, new Path(outputDir));
	
		
        Path documentsSequencePath = new Path(inputDir, "New-York");
        Path tokenizedDocumentsPath = new Path(outputDir, DocumentProcessor.TOKENIZED_DOCUMENT_OUTPUT_FOLDER);
        Path tfidfPath = new Path(outputDir, "tfidf");
        Path termFrequencyVectorsPath = new Path("/tmp/mahout-vector", DictionaryVectorizer.DOCUMENT_VECTOR_OUTPUT_FOLDER);
		
        logger.info("Start calcuating TfIdf");
		calculateTfIdf(outputDir, documentsSequencePath, tokenizedDocumentsPath, termFrequencyVectorsPath, tfidfPath);
		//DocumentProcessor.tokenizeDocuments(new Path(inputDir),analyzer.getClass().asSubclass(Analyzer.class),tokenizedPath, conf);
		//DictionaryVectorizer.createTermFrequencyVectors(tokenizedPath,
		//		new Path(outputDir), outputDir, conf, minSupport, maxNGramSize, minLLRValue, 2, true, reduceTasks,chunkSize, sequentialAccessOutput, false);
		//TFIDFConverter.processTfIdf(new Path(outputDir, DictionaryVectorizer.DOCUMENT_VECTOR_OUTPUT_FOLDER),
		//		new Path(outputDir), conf, chunkSize, minDf, maxDFPercent, norm, true, sequentialAccessOutput, false, reduceTasks);
		
		logger.info("End calculating");
		Path vectorsFolder = new Path(outputDir, "tfidf-vectors");
		Path canopyCentroids = new Path(outputDir ,"canopy-centroids");
				
		Path clusterOutput = new Path(outputDir , "clusters");
		CanopyDriver.run(vectorsFolder, canopyCentroids,new EuclideanDistanceMeasure(), 250, 120, false, 0, false);
		KMeansDriver.run(conf, vectorsFolder, new Path(canopyCentroids, "clusters-0"), clusterOutput, 0.01, 20, true, 0, false);
						
		SequenceFile.Reader reader = new SequenceFile.Reader(fs, new Path(clusterOutput + Cluster.CLUSTERS_DIR + "/part-00000"), conf);
		IntWritable key = new IntWritable();
		WeightedVectorWritable value = new WeightedVectorWritable();
		while (reader.next(key, value)) {
			System.out.println(key.toString() + " belongs to cluster " + value.toString());
		}
		reader.close();
	}*/
}
			

/*


package com.technobium;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.mahout.clustering.canopy.CanopyDriver;
import org.apache.mahout.clustering.fuzzykmeans.FuzzyKMeansDriver;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterable;
import org.apache.mahout.vectorizer.DictionaryVectorizer;
import org.apache.mahout.vectorizer.DocumentProcessor;
import org.apache.mahout.vectorizer.common.PartialVectorMerger;
import org.apache.mahout.vectorizer.tfidf.TFIDFConverter;

public class ClusteringDemo {

    String outputFolder;
    Configuration configuration;
    FileSystem fileSystem;
    Path documentsSequencePath;
    Path tokenizedDocumentsPath;
    Path tfidfPath;
    Path termFrequencyVectorsPath;

    public static void main(String args[]) throws Exception {
        ClusteringDemo tester = new ClusteringDemo();

        tester.createTestDocuments();
        tester.calculateTfIdf();
        tester.clusterDocs();

        tester.printSequenceFile(tester.documentsSequencePath);

        System.out.println("\n Clusters: ");
        tester.printSequenceFile(new Path(tester.outputFolder
                + "clusters/clusteredPoints/part-m-00000"));
    }

    public ClusteringDemo() throws IOException {
        configuration = new Configuration();
        fileSystem = FileSystem.get(configuration);

        outputFolder = "output/";
        documentsSequencePath = new Path(outputFolder, "sequence");
        tokenizedDocumentsPath = new Path(outputFolder,
                DocumentProcessor.TOKENIZED_DOCUMENT_OUTPUT_FOLDER);
        tfidfPath = new Path(outputFolder + "tfidf");
        termFrequencyVectorsPath = new Path(outputFolder
                + DictionaryVectorizer.DOCUMENT_VECTOR_OUTPUT_FOLDER);
    }

    public void createTestDocuments() throws IOException {
        SequenceFile.Writer writer = new SequenceFile.Writer(fileSystem,
                configuration, documentsSequencePath, Text.class, Text.class);

        Text id1 = new Text("Document 1");
        Text text1 = new Text("John saw a red car.");
        writer.append(id1, text1);

        Text id2 = new Text("Document 2");
        Text text2 = new Text("Marta found a red bike.");
        writer.append(id2, text2);

        Text id3 = new Text("Document 3");
        Text text3 = new Text("Don need a blue coat.");
        writer.append(id3, text3);

        Text id4 = new Text("Document 4");
        Text text4 = new Text("Mike bought a blue boat.");
        writer.append(id4, text4);

        Text id5 = new Text("Document 5");
        Text text5 = new Text("Albert wants a blue dish.");
        writer.append(id5, text5);

        Text id6 = new Text("Document 6");
        Text text6 = new Text("Lara likes blue glasses.");
        writer.append(id6, text6);

        Text id7 = new Text("Document 7");
        Text text7 = new Text("Donna, do you have red apples?");
        writer.append(id7, text7);

        Text id8 = new Text("Document 8");
        Text text8 = new Text("Sonia needs blue books.");
        writer.append(id8, text8);

        Text id9 = new Text("Document 9");
        Text text9 = new Text("I like blue eyes.");
        writer.append(id9, text9);

        Text id10 = new Text("Document 10");
        Text text10 = new Text("Arleen has a red carpet.");
        writer.append(id10, text10);

        writer.close();
    }

    
    void clusterDocs() throws ClassNotFoundException, IOException,
            InterruptedException {
        String vectorsFolder = outputFolder + "tfidf/tfidf-vectors/";
        String canopyCentroids = outputFolder + "canopy-centroids";
        String clusterOutput = outputFolder + "clusters";

        FileSystem fs = FileSystem.get(configuration);
        Path oldClusterPath = new Path(clusterOutput);

        if (fs.exists(oldClusterPath)) {
            fs.delete(oldClusterPath, true);
        }

        CanopyDriver.run(new Path(vectorsFolder), new Path(canopyCentroids),
                new EuclideanDistanceMeasure(), 20, 5, true, 0, true);

        FuzzyKMeansDriver.run(new Path(vectorsFolder), new Path(
                canopyCentroids, "clusters-0-final"), new Path(clusterOutput),
                0.01, 20, 2, true, true, 0, false);
    }

    void printSequenceFile(Path path) {
        SequenceFileIterable<Writable, Writable> iterable = new SequenceFileIterable<Writable, Writable>(
                path, configuration);
        for (Pair<Writable, Writable> pair : iterable) {
            System.out
                    .format("%10s -> %s\n", pair.getFirst(), pair.getSecond());
        }
    }
}*/
				