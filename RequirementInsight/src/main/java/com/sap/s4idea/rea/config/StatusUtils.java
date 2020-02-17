package com.sap.s4idea.rea.config;

import java.io.*;

public class StatusUtils {

    private static String fileName = System.getProperty("user.dir") + "/processedWechat.txt";

    public static void saveStatus(Status status) throws IOException {
        File file = new File(fileName);
        if (!file.exists())
            file.createNewFile();

        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        out.write(status.toString());
        out.flush(); // 把缓存区内容压入文件
        out.close(); // 最后记得关闭文件
    }

    public static Status getStatus() throws IOException, ClassNotFoundException {
        File file = new File(fileName);
        if (!file.exists())
            file.createNewFile();

        Status status = new Status();

        InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
        BufferedReader br = new BufferedReader(reader);
        String line = "";
        line = br.readLine();
        while (line != null) {
            if (!line.isEmpty()) {
                String[] arr = line.split("=");
                if (arr.length == 2) {
                    switch (arr[0]) {
                        case "postVoiceId":
                            status.setPostVoiceId(Long.parseLong(arr[1]));
                            break;
                        case "commentVoiceId":
                            status.setCommentVoiceId(Long.parseLong(arr[1]));
                            break;
                        case "postSubmitId":
                            status.setPostSubmitId(Long.parseLong(arr[1]));
                            break;
                        case "commentSubmitId":
                            status.setCommentSubmitId(Long.parseLong(arr[1]));
                            break;
                    }
                }
            }

            line = br.readLine();
        }
        br.close();
        reader.close();
        return status;
    }

    public static class Status implements Serializable {
        private long postVoiceId;
        private long commentVoiceId;
        private long postSubmitId;
        private long commentSubmitId;


        public long getPostVoiceId() {
            return postVoiceId;
        }

        public void setPostVoiceId(long postVoiceId) {
            this.postVoiceId = postVoiceId;
        }

        public long getCommentVoiceId() {
            return commentVoiceId;
        }

        public void setCommentVoiceId(long commentVoiceId) {
            this.commentVoiceId = commentVoiceId;
        }

        public long getPostSubmitId() {
            return postSubmitId;
        }

        public void setPostSubmitId(long postSubmitId) {
            this.postSubmitId = postSubmitId;
        }

        public long getCommentSubmitId() {
            return commentSubmitId;
        }

        public void setCommentSubmitId(long commentSubmitId) {
            this.commentSubmitId = commentSubmitId;
        }

        @Override
        public String toString() {
            return "postVoiceId=" + postVoiceId +
                    "\r\ncommentVoiceId=" + commentVoiceId +
                    "\r\npostSubmitId=" + postSubmitId +
                    "\r\ncommentSubmitId=" + commentSubmitId;
        }
    }
}
