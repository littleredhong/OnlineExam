package com.exam.controller;

import java.util.Arrays;
import java.util.List;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.exam.model.Examination;
import com.exam.model.Grade;
import com.exam.model.Question;
import com.exam.model.User;
import com.exam.service.ExaminationService;
import com.exam.service.GradeService;
import com.exam.service.UserService;
import com.exam.util.CoreConst;
import com.exam.util.PageUtil;
import com.exam.util.ResultUtil;
import com.exam.vo.GradeConditionVo;
import com.exam.vo.base.PageResultVo;
import com.exam.vo.base.ResponseVo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

@Controller
@RequestMapping("grade")
public class GradeController {
	
	@Autowired
	private GradeService gradeService;
	@Autowired
	private ExaminationService examService;
	@Autowired
	private UserService userService;
	
	@PostMapping("list")
	@ResponseBody
	public PageResultVo loadGrade(GradeConditionVo gradeConditionVo, Integer limit, Integer offset) {
		PageHelper.startPage(PageUtil.getPageNo(limit, offset),limit);
		User user = (User)SecurityUtils.getSubject().getPrincipal();
		List<String> roleList = userService.selectRoleByUserId(user.getUserId());
		if(!roleList.contains("超级管理员")) {
			if(roleList.contains("老师")) {
				gradeConditionVo.setAuthor(user.getNickname());
			}else {
				gradeConditionVo.setUserId(user.getUserId());
			}
		}
		List<Grade> gradeList = gradeService.findByCondition(gradeConditionVo);
		PageInfo<Grade> pages = new PageInfo<>(gradeList);
		return ResultUtil.table(gradeList, pages.getTotal(), pages);
	}
	
	/**
	 * 批阅试卷
	 * @param grade
	 * @return
	 */
	@GetMapping("/mark")
	public String mark(Model model, Integer id) {
		Grade grade = gradeService.selectByPrimaryKey(id);
		List<String> answerStr = Arrays.asList(grade.getAnswerJson().split("~_~"));
		Examination examination = examService.queryByExamId(grade.getExamId());
		long examTime = (examination.getEndTime().getTime()-examination.getStartTime().getTime())/(1000*60);
		examination.setExamTime(examTime);
		List<Question> questions = examination.getQuestions();
		for(int i = 0; i < questions.size(); i++) {
			Question question = questions.get(i);
			question.setStuAnswer(answerStr.get(i));
		}
		User user = userService.selectByUserId(grade.getUserId());
		grade.setExamination(examination);
		grade.setUser(user);
		model.addAttribute("grade", grade);
		return "grade/mark";
	}
	
	@PostMapping("/mark")
	@ResponseBody
	public ResponseVo mark(Grade grade) {
		try {
			Grade obj = gradeService.selectById(grade.getId());
			grade.setResult(obj.getAutoResult()+grade.getManulResult());
			if(grade.getResult() == 0) {
				grade.setStatus(CoreConst.EXAM_END);
			}
			int userScore = grade.getResult();
			int examScore = obj.getExamination().getTotalScore();
			float score = (float)userScore/examScore;
			if(score < 0.6) {
				grade.setStatus(CoreConst.EXAM_END);
			}else if(score >= 0.9) {
				grade.setStatus(CoreConst.EXAM_FINE);
			}else if(score >= 0.8) {
				grade.setStatus(CoreConst.EXAM_GOOD);
			}else if(score >= 0.6) {
				grade.setStatus(CoreConst.EXAM_STANDARD);
			}
			gradeService.updateNotNull(grade);
			return ResultUtil.success("批阅成功");
		} catch (Exception e) {
			return ResultUtil.success("批阅失败");
		}
	}
	
	/**
	 * 查看成绩详情
	 * @param model
	 * @param id
	 * @return
	 */
	
	@GetMapping("/detail")
	public String detail(Model model, Integer id) {
		Grade grade = gradeService.selectByPrimaryKey(id);
		List<String> answerStr = Arrays.asList(grade.getAnswerJson().split("~_~"));
		Examination examination = examService.queryByExamId(grade.getExamId());
		long examTime = (examination.getEndTime().getTime()-examination.getStartTime().getTime())/(1000*60);
		examination.setExamTime(examTime);
		List<Question> questions = examination.getQuestions();
		for(int i = 0; i < questions.size(); i++) {
			Question question = questions.get(i);
			question.setStuAnswer(answerStr.get(i));
		}
		User user = userService.selectByUserId(grade.getUserId());
		grade.setExamination(examination);
		grade.setUser(user);
		model.addAttribute("grade", grade);
		return "grade/detail";
	}
	
	
	/**
	 * 单个删除成绩
	 * @param id
	 * @return
	 */
	@PostMapping("/delete")
	@ResponseBody
	public ResponseVo delete(Integer id) {
		int i = gradeService.deleteBatch(new Integer[] {id});
		if(i > 0) {
			return ResultUtil.success("删除成绩成功");
		}else {
			return ResultUtil.error("删除成绩失败");
		}
	}
	
	/**
	 * 批量删除成绩
	 * @param ids
	 * @return
	 */
	@PostMapping("/batch/delete")
	@ResponseBody
	public ResponseVo deleteBatch(@RequestParam("ids[]") Integer[] ids) {
		int i = gradeService.deleteBatch(ids);
		if(i > 0) {
			return ResultUtil.success("批量删除成绩成功");
		}else {
			return ResultUtil.error("批量删除成绩失败");
		}
	}
	
}
