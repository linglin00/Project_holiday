package WebScraper;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.mahout.clustering.classify.WeightedPropertyVectorWritable;
import org.apache.mahout.clustering.classify.WeightedVectorWritable;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.Vector;

public class DocLabel {
	@SuppressWarnings("resource")
	public static void main(String[] argv) throws Exception {
		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(conf);
		Path clusterOutput = new Path("/home/ling/Downloads");
		
		@SuppressWarnings("deprecation")
		SequenceFile.Reader reader = new SequenceFile.Reader(fs, new Path(clusterOutput + "/part-m-00000"), conf);
        IntWritable key = new IntWritable();
        WeightedPropertyVectorWritable value = new WeightedPropertyVectorWritable();
        while (reader.next(key, value)) {
        	String pointName = "";
        	Vector theVec = value.getVector();
            if (theVec instanceof NamedVector)
            	pointName = ((NamedVector) theVec).getName();
            System.out.println(pointName + " belongs to cluster " + key.toString());
        }
        reader.close();
		/*WeightedVectorWritable value = new WeightedVectorWritable();
		while (reader.next(key, value)) {
			NamedVector vector = (NamedVector)
			value.getVector();
			String vectorName = vector.getName();
			System.out.println(vectorName + " belongs to cluster "+key.toString());
		}*/
	}
}
