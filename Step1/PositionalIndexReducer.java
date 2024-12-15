import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class PositionalIndexReducer extends Reducer<Text, Text, Text, Text> {
    private Text result = new Text();

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {
        Map<String, TreeSet<Integer>> docPositionsMap = new HashMap<>();

        for (Text val : values) {
            String[] docPosition = val.toString().split("@");
            String docId = docPosition[0];
            int position = Integer.parseInt(docPosition[1]);
            if (!docPositionsMap.containsKey(docId)) {
                docPositionsMap.put(docId, new TreeSet<Integer>());
            }
            docPositionsMap.get(docId).add(position);
        }
        StringBuilder aggregatedPositions = new StringBuilder();
        for (Map.Entry<String, TreeSet<Integer>> entry : docPositionsMap.entrySet()) {
            if (aggregatedPositions.length() > 0) {
                aggregatedPositions.append("; ");
            }
            aggregatedPositions.append(entry.getKey()).append(": ");
            aggregatedPositions.append(entry.getValue().toString().replaceAll("[\\[\\] ]", ""));
        }
        result.set(aggregatedPositions.toString());
        context.write(key, result);
    }
}