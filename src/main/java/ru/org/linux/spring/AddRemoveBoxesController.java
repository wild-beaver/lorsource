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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import ru.org.linux.site.AccessViolationException;
import ru.org.linux.site.DefaultProfile;
import ru.org.linux.site.Template;
import ru.org.linux.spring.validators.EditBoxesFormValidator;
import ru.org.linux.storage.StorageException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

@Controller
@SessionAttributes("allboxes")
public class AddRemoveBoxesController extends ApplicationObjectSupport {
  @RequestMapping(value = {"/remove-box.jsp", "/add-box.jsp"}, method = RequestMethod.GET)
  public ModelMap showRemove(@RequestParam String tag,
                             @RequestParam(required = false) Integer pos,
                             ServletRequest request)
    throws AccessViolationException {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    ModelMap result = new ModelMap();
    EditBoxesForm form = new EditBoxesForm();
    form.setPosition(pos);
    form.setTag(tag);
    result.addAttribute("form", form);
    return result;
  }

  @RequestMapping(value = "/remove-box.jsp", method = RequestMethod.POST)
  public String doRemove(@ModelAttribute("form") EditBoxesForm form, BindingResult result,
                         SessionStatus status, HttpServletRequest request)
    throws Exception {
    Template t = Template.getTemplate(request);

    if (!t.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    new EditBoxesFormValidator().validate(form, result);
    ValidationUtils.rejectIfEmptyOrWhitespace(result, "position", "position.empty", "Не указанa позиция бокслета");
    if (result.hasErrors()) {
      return "remove-box";
    }

    if (result.hasErrors()) {
      return "remove-box";
    }

    String objectName = getObjectName(form, request);
    List<String> boxlets = new ArrayList<String>(t.getProf().getList(objectName));

    if (!boxlets.isEmpty()) {
      if (boxlets.size() > form.position) {
        boxlets.remove(form.position.intValue());
        t.getProf().setList(objectName, boxlets);
        t.writeProfile(t.getProfileName());
      }
    }
    
    status.setComplete();
    return "redirect:/edit-boxes.jsp";
  }

  @ModelAttribute("allboxes")
  public Set<String> getAllBoxes() {
    return DefaultProfile.getAllBoxes();
  }

  @RequestMapping(value = "/add-box.jsp", method = RequestMethod.POST)
  public String doAdd(@ModelAttribute("form") EditBoxesForm form, BindingResult result,
                      SessionStatus status, HttpServletRequest request)
    throws IOException,
          AccessViolationException, StorageException {

    new EditBoxesFormValidator().validate(form, result);
    ValidationUtils.rejectIfEmptyOrWhitespace(result, "boxName", "boxName.empty", "Не выбран бокслет");
    if (StringUtils.isNotEmpty(form.getBoxName()) && !DefaultProfile.isBox(form.getBoxName())) {
      result.addError(new FieldError("boxName", "boxName.invalid", "Неверный бокслет"));
    }
    if (result.hasErrors()) {
      return "add-box";
    }
    Template t = Template.getTemplate(request);

    if (result.hasErrors()) {
      return "add-box";
    }

    if (form.getPosition() == null) {
      form.setPosition(0);
    }

    String objectName = getObjectName(form, request);
    List<String> boxlets = new ArrayList<String>(t.getProf().getList(objectName));

    CollectionUtils.filter(boxlets, DefaultProfile.getBoxPredicate());

    if (boxlets.size() > form.position) {
      boxlets.add(form.position, form.boxName);
    } else {
      boxlets.add(form.boxName);
    }
    
    t.getProf().setList(objectName, boxlets);
    t.writeProfile(t.getProfileName());

    status.setComplete();
    return "redirect:/edit-boxes.jsp";
  }

  private static String getObjectName(EditBoxesForm form, HttpServletRequest request)  {
    String objectName;
    if ("left".equals(form.getTag())) {
      if (EditBoxesController.getThreeColumns(request)) {
        objectName = "main3-1";
      } else {
        objectName = "main2";
      }
    } else {
      objectName = "main3-2";
    }
    return objectName;
  }

  public static class EditBoxesForm {
    private Integer position;
    private String tag;
    private String password;
    private String boxName;

    public Integer getPosition() {
      return position;
    }

    public void setPosition(Integer position) {
      this.position = position;
    }

    public String getTag() {
      return tag;
    }

    public void setTag(String tag) {
      this.tag = tag;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getBoxName() {
      return boxName;
    }

    public void setBoxName(String boxName) {
      this.boxName = boxName;
    }
  }
}
