package com.kean.service;

import com.kean.vo.CampusVO;
import com.kean.vo.CourseVO;
import com.kean.vo.SchoolVO;

import java.util.List;

public interface CatalogService {

    List<SchoolVO> listSchools();

    List<CampusVO> listCampuses(Long schoolId);

    List<CourseVO> listCourses(Long schoolId);
}
