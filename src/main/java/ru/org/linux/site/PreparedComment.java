/*
 * Copyright 1998-2010 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.site;

import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.util.bbcode.ParserResult;
import ru.org.linux.util.bbcode.ParserUtil;

public class PreparedComment {
  private final Comment comment;
  private final User author;
  private final String processedMessage;
  private final User replyAuthor;

  public PreparedComment(Comment comment, User author, String processedMessage, User replyAuthor) {
    this.comment = comment;
    this.author = author;
    this.processedMessage = processedMessage;
    this.replyAuthor = replyAuthor;
  }

  public static ParserResult getProcessedMessage(UserDao userDao, String message) {
    return ParserUtil.bb2xhtm(message, true, true, "", userDao);
  }

  public Comment getComment() {
    return comment;
  }

  public User getAuthor() {
    return author;
  }

  public String getProcessedMessage() {
    return processedMessage;
  }

  public User getReplyAuthor() {
    return replyAuthor;
  }
}
