package blasd.apex.spark.run;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blasd.apex.hadoop.ApexHadoopHelper;
import blasd.apex.parquet.ParquetStreamFactory;

public class RunCsvToParquet {

	protected static final Logger LOGGER = LoggerFactory.getLogger(RunCsvToParquet.class);

	public static void main(String[] args) throws Exception {
		if (!ApexHadoopHelper.isHadoopReady()) {
			throw new IllegalStateException("Hadoop is not ready");
		}

		if (args == null || args.length != 2) {
			throw new IllegalArgumentException(
					"We expected 2 arguments: path to CSV file and path to fodler where to write .parquet");
		}

		Path tmpPath = Paths.get(args[0]);
		Path tmpParquetPath = Paths.get(args[1]);

		csvToParquet(tmpPath, tmpParquetPath);
	}

	public static void csvToParquet(Path csvPath, Path parquetTargetPath) throws FileNotFoundException, IOException {
		LOGGER.info("About to convert {} into folder {}", csvPath, parquetTargetPath);

		if (parquetTargetPath.toFile().isFile()) {
			throw new IllegalArgumentException(
					"Can not write parquet files in folder which is already a file: " + parquetTargetPath);
		}

		// http://stackoverflow.com/questions/38008330/spark-error-a-master-url-must-be-set-in-your-configuration-when-submitting-a
		// https://jaceklaskowski.gitbooks.io/mastering-apache-spark/spark-local.html
		try (SparkSession spark =
				SparkSession.builder().appName("CsvToParquet").config("spark.master", "local[*]").getOrCreate()) {

			try (JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext())) {
				// http://bytepadding.com/big-data/spark/read-write-parquet-files-using-spark/
				SQLContext sqlContext = spark.sqlContext();
				Dataset<Row> inputDf = sqlContext.read().csv(csvPath.toAbsolutePath().toString());

				inputDf.write().parquet(parquetTargetPath.toAbsolutePath().toString());
			}
		}

		Arrays.stream(
				parquetTargetPath.toFile().listFiles(file -> file.isFile() && file.getName().endsWith(".parquet")))
				.forEach(file -> {
					LOGGER.info("Parquet file: {}", file);

					try {
						ParquetStreamFactory.readParquetAsStream(file.toPath(), Collections.emptyMap()).forEach(row -> {
							LOGGER.info("Row: {}", row);
						});
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
	}
}