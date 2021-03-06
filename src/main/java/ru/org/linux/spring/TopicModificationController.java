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

package ru.org.linux.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.site.*;
import ru.org.linux.spring.dao.GroupDao;
import ru.org.linux.spring.dao.MessageDao;
import ru.org.linux.spring.dao.SectionDao;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

@Controller
public class TopicModificationController extends ApplicationObjectSupport {
  @Autowired
  private PrepareService prepareService;

  @Autowired
  private MessageDao messageDao;

  @Autowired
  private SectionDao sectionDao;

  @Autowired
  private GroupDao groupDao;

  @RequestMapping(value="/setpostscore.jsp", method= RequestMethod.GET)
  public ModelAndView showForm(
    ServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    return new ModelAndView("setpostscore", "message", messageDao.getById(msgid));
  }

  @RequestMapping(value="/setpostscore.jsp", method= RequestMethod.POST)
  public ModelAndView modifyTopic(
    ServletRequest request,
    @RequestParam int msgid,
    @RequestParam int postscore,
    @RequestParam(defaultValue="false") boolean sticky,
    @RequestParam(defaultValue="false") boolean notop,
    @RequestParam(defaultValue="false") boolean minor
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    if (postscore < Message.POSTSCORE_UNRESTRICTED) {
      throw new UserErrorException("invalid postscore " + postscore);
    }

    if (postscore > Message.POSTSCORE_UNRESTRICTED && postscore < Message.POSTSCORE_REGISTERED_ONLY) {
      throw new UserErrorException("invalid postscore " + postscore);
    }

    if (postscore > Message.POSTSCORE_MODERATORS_ONLY) {
      throw new UserErrorException("invalid postscore " + postscore);
    }

    User user = tmpl.getCurrentUser();
    user.checkCommit();

    Message msg = messageDao.getById(msgid);

    messageDao.setTopicOptions(msg, postscore, sticky, notop, minor);

    StringBuilder out = new StringBuilder();

    if (msg.getPostScore() != postscore) {
      out.append("Установлен новый уровень записи: ").append(getPostScoreInfoFull(postscore)).append("<br>");
      logger.info("Установлен новый уровень записи " + postscore + " для " + msgid + " пользователем " + user.getNick());
    }

    if (msg.isSticky() != sticky) {
      out.append("Новое значение sticky: ").append(sticky).append("<br>");
      logger.info("Новое значение sticky: " + sticky);
    }

    if (msg.isNotop() != notop) {
      out.append("Новое значение notop: ").append(notop).append("<br>");
      logger.info("Новое значение notop: " + notop);
    }

    ModelAndView mv = new ModelAndView("action-done");
    mv.getModel().put("message", "Данные изменены");
    mv.getModel().put("bigMessage", out.toString());
    mv.getModel().put("link", msg.getLink());

    return mv;
  }

  @RequestMapping(value="/mtn.jsp", method=RequestMethod.GET)
  public ModelAndView moveTopicForm(
    ServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new IllegalAccessException("Not authorized");
    }

    ModelAndView mv = new ModelAndView("mtn");

    Message message = messageDao.getById(msgid);
    Section section = sectionDao.getSection(message.getSectionId());

    mv.getModel().put("message", message);

    mv.getModel().put("groups", groupDao.getGroups(section));

    return mv;
  }

  @RequestMapping(value="/mt.jsp", method=RequestMethod.POST)
  public ModelAndView moveTopic(
    ServletRequest request,
    @RequestParam int msgid,
    @RequestParam("moveto") int newgr
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    Message msg = messageDao.getById(msgid);

    if (msg.isDeleted()) {
      throw new AccessViolationException("Сообщение удалено");
    }

    Group newGrp = groupDao.getGroup(newgr);

    messageDao.moveTopic(msg, newGrp, tmpl.getCurrentUser());

    logger.info("topic " + msgid + " moved" +
            " by " + tmpl.getNick() + " from news/forum " + msg.getGroupTitle() + " to forum " + newGrp.getTitle());

    return new ModelAndView(new RedirectView(msg.getLinkLastmod()));
  }

  @RequestMapping(value="/mt.jsp", method=RequestMethod.GET)
  public ModelAndView moveTopicFormForum(
    ServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new IllegalAccessException("Not authorized");
    }

    ModelAndView mv = new ModelAndView("mtn");

    Message message = messageDao.getById(msgid);

    mv.getModel().put("message", message);

    Section section = sectionDao.getSection(Section.SECTION_FORUM);

    mv.getModel().put("groups", groupDao.getGroups(section));

    return mv;
  }

  @RequestMapping(value = "/uncommit.jsp", method = RequestMethod.GET)
  public ModelAndView uncommitForm(
    HttpServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    Message message = messageDao.getById(msgid);

    checkUncommitable(message);

    ModelAndView mv = new ModelAndView("uncommit");
    mv.getModel().put("message", message);
    mv.getModel().put("preparedMessage", prepareService.prepareMessage(message, true));

    return mv;
  }

  @RequestMapping(value="/uncommit.jsp", method=RequestMethod.POST)
  public ModelAndView uncommit(
    HttpServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    Message message = messageDao.getById(msgid);

    checkUncommitable(message);

    messageDao.uncommit(message);

    logger.info("Отменено подтверждение сообщения " + msgid + " пользователем " + tmpl.getNick());

    return new ModelAndView("action-done", "message", "Подтверждение отменено");
  }

  private static void checkUncommitable(Message message) throws AccessViolationException {
    if (message.isExpired()) {
      throw new AccessViolationException("нельзя восстанавливать устаревшие сообщения");
    }

    if (message.isDeleted()) {
      throw new AccessViolationException("сообщение удалено");
    }

    if (!message.isCommited()) {
      throw new AccessViolationException("сообщение не подтверждено");
    }
  }

  public static String getPostScoreInfoFull(int postscore) {
    String info = Message.getPostScoreInfo(postscore);
    if (info.isEmpty()) {
      return "без ограничений";
    } else {
      return info;
    }
  }
}
