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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.site.*;
import ru.org.linux.spring.dao.MessageDao;
import ru.org.linux.spring.dao.PollDao;
import ru.org.linux.spring.dao.VoteDTO;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class VoteController {

  @Autowired
  private PollDao pollDao;

  @Autowired
  private MessageDao messageDao;

  @RequestMapping(value="/vote.jsp", method= RequestMethod.POST)
  public ModelAndView vote(
    ServletRequest request,
    @RequestParam("vote") int[] votes,
    @RequestParam("voteid") int voteid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();

    Poll poll = pollDao.getCurrentPoll();
    Message msg = messageDao.getById(poll.getTopicId());

    if (voteid != poll.getId()) {
      throw new BadVoteException("голосовать можно только в текущий опрос");
    }

    if (votes==null || votes.length==0) {
      throw new BadVoteException("ничего не выбрано");
    }

    if (!poll.isMultiSelect() && votes.length!=1) {
      throw new BadVoteException("этот опрос допускает только один вариант ответа");
    }

    pollDao.updateVotes(voteid, votes, user);

    StringBuilder url = new StringBuilder();

    for (int vote : votes) {
      if (url.length()==0) {
        url.append('?');
      } else {
        url.append('&');
      }

      url.append("highlight=");
      url.append(Integer.toString(vote));
    }

    return new ModelAndView(new RedirectView(msg.getLink() + url));
  }

  @RequestMapping(value="/vote-vote.jsp", method=RequestMethod.GET)
  public ModelAndView showForm(
    @RequestParam("msgid") int msgid,
    HttpServletRequest request
  ) throws Exception {
    if (!Template.isSessionAuthorized(request.getSession())) {
      throw new AccessViolationException("Not authorized");
    }

    Map<String, Object> params = new HashMap<String, Object>();

    Message msg = messageDao.getById(msgid);
    params.put("message", msg);

    Poll poll = pollDao.getPollByTopicId(msgid);
    if (!poll.isCurrent()) {
      throw new BadVoteException("голосовать можно только в текущий опрос");
    }
    params.put("poll", poll);

    List<VoteDTO> votes = pollDao.getVoteDTO(poll.getId());
    params.put("votes", votes);

    return new ModelAndView("vote-vote", params);
  }

  @RequestMapping("/view-vote.jsp")
  public ModelAndView viewVote(@RequestParam("vote") int voteid) throws Exception {
    Poll poll = pollDao.getPoll(voteid);
    return new ModelAndView(new RedirectView("/jump-message.jsp?msgid=" + poll.getTopicId()));
  }
}