package NIO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class NIOServer implements Runnable {
    private ServerSocketChannel server;
    private Selector selector;
    private final String serverFilesPath = "./common/src/main/resources/serverFiles/user1/";

    public NIOServer() throws IOException {
        server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(8189));
        server.configureBlocking(false);
        selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        try {
            System.out.println("server started");
            while (server.isOpen()) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        System.out.println("client accepted");
                        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
                        channel.configureBlocking(false);
                        channel.register(selector, SelectionKey.OP_READ);
                    }
                    if (key.isReadable()) {
                        // TODO: 7/23/2020 fileStorage handle
                        System.out.println("read key");
                        ByteBuffer buffer = ByteBuffer.allocate(80);
                        int count = ((SocketChannel)key.channel()).read(buffer);
                        if (count == -1) {
                            key.channel().close();
                            break;
                        }
                        buffer.flip();

                        StringBuilder s = new StringBuilder();
                        while (buffer.hasRemaining()) {
                            s.append((char)buffer.get());
                        }

                        String [] command = s.toString().split(" ", 2);

                        if (command[0].equals("./download")) {
                            try {
                                sendFile(command[1], key);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        if (command[0].equals("./upload")) {
                            try {
                                uploadFileFromClient(key, command[1]);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadFileFromClient(SelectionKey key, String fileName) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buff = ByteBuffer.allocate(8192);
        File f = new File(serverFilesPath + fileName);
        try (FileChannel fileChannel = new FileOutputStream(f, true).getChannel()) {
            int read;
            int totalRead = 0;
            while ((read = channel.read(buff)) > 0) {
                System.out.println("current read: " + read);
                totalRead += read;
                buff.flip();
                fileChannel.write(buff);
                buff.clear();
            }
            System.out.println("total server read: " + totalRead);
        }
    }

    public void sendFile(String fileName, SelectionKey sk) throws IOException {
        File file = new File(serverFilesPath + fileName);
        if(file.exists()) {
            SocketChannel ch = (SocketChannel) sk.channel();
            ch.write(ByteBuffer.wrap("OK".getBytes()));

            System.out.println("writing");
            FileChannel fileChannel = new FileInputStream(file).getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(12);
            int bytes;
            buffer.clear();
            int total = 0;
            while ((bytes = fileChannel.read(buffer)) > 0) {
                total += bytes;
                buffer.flip();
                System.out.println("wrote " + bytes + " bytes");
                //System.out.println(StandardCharsets.UTF_8.decode(buffer).toString());
                ch.write(buffer);
                buffer.clear();
            }
            System.out.println("total wrote: " + total);
            ch.write(ByteBuffer.wrap("|".getBytes()));
        }
    }

    public static void main(String[] args) throws IOException {
        new Thread(new NIOServer()).start();
    }
}
