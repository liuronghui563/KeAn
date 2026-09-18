package com.kean.service;

import com.kean.dto.ApplyRequest;
import com.kean.vo.ApplicationVO;
import com.kean.vo.TaskVO;

import java.util.List;

public interface ApplicationService {

    TaskVO apply(Long taskId, ApplyRequest request);

    List<ApplicationVO> listByTask(Long taskId);

    TaskVO accept(Long applicationId);

    TaskVO reject(Long applicationId);

    TaskVO withdraw(Long applicationId);
}
