package ds.sync.taskone.multiThreadedFileCopy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;




/**
 * Demonstrates copying a file using a single thread. Note that this class is declared final because
 * it provides an application entry point, and therefore not supposed to be extended.
 */
public final class FileCopyMultiThreaded {

	/**
	 * Copies a file. The first argument is expected to be a qualified source file name, the second
	 * a qualified target file name.
	 * @param args the VM arguments
	 * @throws IOException if there's an I/O related problem
	 */
	static public void main (final String[] args) throws IOException {
		final Path sourcePath = Paths.get(args[0]);
		if (!Files.isReadable(sourcePath)) throw new IllegalArgumentException(sourcePath.toString());

		final Path sinkPath = Paths.get(args[1]);
		if (sinkPath.getParent() != null && !Files.isDirectory(sinkPath.getParent())) throw new IllegalArgumentException(sinkPath.toString());

//		Files.copy(sourcePath, sinkPath, StandardCopyOption.REPLACE_EXISTING);
		Path copiedFilePath = Paths.get(args[1] + "/" + sourcePath.toFile().getName());
		deleteOldCreateNewFile(copiedFilePath);
		
		PipedOutputStream pipedOutput = new PipedOutputStream();
		PipedInputStream pipedInputStream = new PipedInputStream(pipedOutput);
		
		Transporter transporter1 = new Transporter(Files.newInputStream(sourcePath), pipedOutput);
		Transporter transporter2 = new Transporter(pipedInputStream, Files.newOutputStream(copiedFilePath));
		
		Thread thread1 = new Thread(transporter1);
		Thread thread2 = new Thread(transporter2);
		
		thread1.start();
		thread2.start();
		
		
		
	}
	
	private static void deleteOldCreateNewFile(Path copiedFilePath) throws IOException {
		Files.deleteIfExists(copiedFilePath);
		Files.createFile(copiedFilePath);
		
	}

	/**
	 * Private static inner class Transporter
	 */
	private static class Transporter implements Runnable{
		
		private InputStream inputStream;
		private OutputStream outputStream;
		
		public Transporter(InputStream inputStream, OutputStream outputStream){
			this.inputStream = inputStream;
			this.outputStream = outputStream;
		}

		public InputStream getInputStream() {
			return inputStream;
		}

		public OutputStream getOutputStream() {
			return outputStream;
		}

		@Override
		public void run() {
			try (InputStream fis = getInputStream()) {
				try (OutputStream fos = getOutputStream()) {
					final byte[] buffer = new byte[0x10000];
					for (int bytesRead = fis.read(buffer); bytesRead != -1; bytesRead = fis.read(buffer)) {
						fos.write(buffer, 0, bytesRead);
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally {
				System.out.println(Thread.currentThread().getName() + " done.");
			}
		}
		
	}
} 
