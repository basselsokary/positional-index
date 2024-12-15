import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class PositionalIndexMapper extends Mapper<LongWritable, Text, Text, Text> {

    private Text word = new Text();
    private Text position = new Text();

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
        FileSplit fileSplit = (FileSplit) context.getInputSplit();
        String docId = fileSplit.getPath().getName(); // Get the filename
        String[] words = value.toString().split("\\s+");

        for (int i = 0; i < words.length; i++) {
            word.set(words[i].toLowerCase());
            position.set(docId + "@" + i);
            context.write(word, position);
        }
    }
}
