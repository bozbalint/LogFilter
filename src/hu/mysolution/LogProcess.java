package hu.mysolution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author bozbalint
 *
 *         It is a java 8 code.
 *
 *         It doesn't have Junit tests as Junit is not part of the standard Java
 *         :-((
 *
 */
public class LogProcess {

	private final static String INPUT_FOLDER = "./input";
	private final static String OUTPUT_FOLDER = "./output";
	private final static String CONFIG_FILE = "config.txt";

	private ExecutorService executor;

	public static void main(String[] args) {

		LogProcess process = new LogProcess();

		Path configFile = Paths.get(CONFIG_FILE);
		Path folderName = Paths.get(INPUT_FOLDER);

		Map<String, String> configs = process.readConfigFile(configFile);
		Map<String, Pattern> patternMap = process.regexpBuilder(configs);
		Map<String, BlockingQueue<String>> queueMap = process.startWriterWorkers(configs.keySet(), OUTPUT_FOLDER);

		process.processLogFiles(folderName, patternMap, queueMap);

		if (process.executor != null)
			process.executor.shutdownNow();
	}

	/**
	 * @param file
	 *            is the path of a config file.
	 * @return the parsed config file. The map key value contains the regexp ID
	 *         which a file name and the value of of the map is the regexp string.
	 */
	private Map<String, String> readConfigFile(Path file) {

		Map<String, String> configMap = new ConcurrentHashMap<>();

		try (Stream<String> stream = Files.lines(file)) {

			configMap = stream.filter(line -> line.contains((CharSequence) ":")).map(s -> s.split(":\\s"))
					.filter(s -> s.length == 2).collect(Collectors.toMap(s -> s[0], s -> s[1]));

		} catch (IOException e) {
			e.printStackTrace();
		}
		return configMap;
	}

	/**
	 * @param configs
	 *            the parsed config file as a map
	 * @return a map which has the regexp id (file name) and the regexp pattern
	 */
	private Map<String, Pattern> regexpBuilder(Map<String, String> configs) {

		Map<String, Pattern> patternMap = new HashMap<>();

		configs.forEach((name, regexp) -> {
			patternMap.put(name, Pattern.compile(regexp));
		});

		return patternMap;
	}

	/**
	 * Once a log file is read and processed the lines of the files are filtered by
	 * regexp and sorted out to regexp specified queue. The other end of the queue a
	 * writer receives the file and write to a file.
	 * 
	 * @param logFolder
	 *            the path of the log file folder which is the input folder
	 * @param patternMap
	 *            contains the regexp id and regexp patterns
	 * @param queueMap
	 *            contains the regexp id and the corresponding blocking queue which
	 *            links to the file writer worker.
	 */
	private void processLogFiles(Path logFolder, Map<String, Pattern> patternMap,
			Map<String, BlockingQueue<String>> queueMap) {

		try (Stream<Path> paths = Files.walk(logFolder)) {
			paths.filter(Files::isRegularFile).forEach(i -> {

				try (Stream<String> fileLines = Files.lines(i)) {

					fileLines.forEach(s -> {

						patternMap.forEach((name, pattern) -> {
							Matcher m = pattern.matcher(s);
							if (m.matches()) {
								try {
									queueMap.get(name).put(s);
								} catch (InterruptedException e) {
									executor.shutdown();
									System.out.println("Worker interrupted, faild to send file to file: " + name);
								}
							}
						});
					});
				} catch (IOException e) {
					System.out.println("Failed to read the file: " + i);
				}

			});
		} catch (IOException e) {
			System.out.println("Failed to read the folder: " + logFolder.getFileName());
		}
	}

	/**
	 * The file write is the slowest amongst all tasks so each file writer has own
	 * worker in a separate thread. A worker gets task via queue.
	 * 
	 * @param outFileNames
	 *            the regexp id which is used as filename
	 * @param folderName
	 *            the path of output folder the place of the generated files
	 * @return a regexp id and blocking queue map which is a link to the specific
	 *         worker
	 */
	private Map<String, BlockingQueue<String>> startWriterWorkers(Set<String> outFileNames, String folderName) {

		int cores = Runtime.getRuntime().availableProcessors();
		executor = Executors.newFixedThreadPool(Math.max(cores, 1));

		Map<String, BlockingQueue<String>> executorMap = new HashMap<>();
		try {
			for (String fileName : outFileNames) {

				Path writeFile = Paths.get(folderName + File.separator + fileName);
				BlockingQueue<String> queue = new LinkedBlockingQueue<String>();

				executor.submit(() -> {
					try (BufferedWriter writer = Files.newBufferedWriter(writeFile)) {
						while (true) {
							try {
								String lineString = queue.take();
								writer.write(lineString);
								writer.flush();
							} catch (InterruptedException e) {
								writer.close();
							}
						}
					}
				});
				executorMap.put(fileName, queue);
			}
		} catch (Exception e) {
			executor.shutdown();
			System.out.println("Failed to write file.");
			e.printStackTrace();
		}
		return executorMap;
	}
}
