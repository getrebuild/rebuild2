/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.approval;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.DataSpecificationNoRollbackException;
import com.rebuild.core.service.approval.*;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/07/05
 */
@Controller
@RequestMapping({"/app/entity/approval/", "/app/RobotApprovalConfig/"})
public class ApprovalControl extends BaseController {

    @RequestMapping("workable")
    public void getWorkable(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final ID recordId = getIdParameterNotNull(request, "record");

        FlowDefinition[] defs = RobotApprovalManager.instance.getFlowDefinitions(recordId, user);
        JSONArray data = new JSONArray();
        for (FlowDefinition d : defs) {
            data.add(d.toJSON("id", "name"));
        }
        writeSuccess(response, data);
    }

    @RequestMapping("state")
    public void getApprovalState(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final ID recordId = getIdParameterNotNull(request, "record");
        final ApprovalStatus status = ApprovalHelper.getApprovalStatus(recordId);

        Map<String, Object> data = new HashMap<>();

        int stateVal = status.getCurrentState().getState();
        data.put("state", stateVal);

        ID useApproval = status.getApprovalId();
        if (useApproval != null) {
            data.put("approvalId", useApproval);
            // 当前审批步骤
            if (stateVal < ApprovalState.APPROVED.getState()) {
                JSONArray current = new ApprovalProcessor(recordId, useApproval).getCurrentStep();
                data.put("currentStep", current);

                for (Object o : current) {
                    JSONObject step = (JSONObject) o;
                    if (user.toLiteral().equalsIgnoreCase(step.getString("approver"))) {
                        data.put("imApprover", true);
                        data.put("imApproveSatate", step.getInteger("state"));
                        break;
                    }
                }
            }

            // 审批中提交人可撤回
            if (stateVal == ApprovalState.PROCESSING.getState()) {
                ID submitter = ApprovalHelper.getSubmitter(recordId);
                if (user.equals(submitter)) {
                    data.put("canCancel", true);
                }
            }
        }
        writeSuccess(response, data);
    }

    @RequestMapping("fetch-nextstep")
    public void fetchNextStep(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final ID recordId = getIdParameterNotNull(request, "record");
        final ID approvalId = getIdParameterNotNull(request, "approval");

        ApprovalProcessor approvalProcessor = new ApprovalProcessor(recordId, approvalId);
        FlowNodeGroup nextNodes = approvalProcessor.getNextNodes();

        JSONArray approverList = new JSONArray();
        for (ID o : nextNodes.getApproveUsers(user, recordId, null)) {
            approverList.add(new Object[]{o, UserHelper.getName(o)});
        }
        JSONArray ccList = new JSONArray();
        for (ID o : nextNodes.getCcUsers(user, recordId, null)) {
            ccList.add(new Object[]{o, UserHelper.getName(o)});
        }

        JSONObject data = new JSONObject();
        data.put("nextApprovers", approverList);
        data.put("nextCcs", ccList);
        data.put("approverSelfSelecting", nextNodes.allowSelfSelectingApprover());
        data.put("ccSelfSelecting", nextNodes.allowSelfSelectingCc());
        data.put("isLastStep", nextNodes.isLastStep());
        data.put("signMode", nextNodes.getSignMode());
        data.put("useGroup", nextNodes.getGroupId());

        // 可修改字段
        JSONArray editableFields = approvalProcessor.getCurrentNode().getEditableFields();
        if (editableFields != null && !editableFields.isEmpty()) {
            JSONArray aform = new FormBuilder(recordId, user).build(editableFields);
            if (aform != null && !aform.isEmpty()) {
                data.put("aform", aform);
            }
        }

        writeSuccess(response, data);
    }

    @RequestMapping("fetch-workedsteps")
    public void fetchWorkedSteps(HttpServletRequest request, HttpServletResponse response) {
        ID recordId = getIdParameterNotNull(request, "record");
        JSONArray allSteps = new ApprovalProcessor(recordId).getWorkedSteps();
        writeSuccess(response, allSteps);
    }

    @RequestMapping("submit")
    public void doSubmit(HttpServletRequest request, HttpServletResponse response) {
        final ID recordId = getIdParameterNotNull(request, "record");
        final ID approvalId = getIdParameterNotNull(request, "approval");

        JSONObject selectUsers = (JSONObject) ServletUtils.getRequestJson(request);

        try {
            boolean success = new ApprovalProcessor(recordId, approvalId).submit(selectUsers);
            if (success) {
                writeSuccess(response);
            } else {
                writeFailure(response, "无效审批流程，请联系管理员配置");
            }
        } catch (ApprovalException ex) {
            writeFailure(response, ex.getMessage());
        }
    }

    @RequestMapping("approve")
    public void doApprove(HttpServletRequest request, HttpServletResponse response) {
        final ID approver = getRequestUser(request);
        final ID recordId = getIdParameterNotNull(request, "record");
        final int state = getIntParameter(request, "state", ApprovalState.REJECTED.getState());

        JSONObject post = (JSONObject) ServletUtils.getRequestJson(request);
        JSONObject selectUsers = post.getJSONObject("selectUsers");
        String remark = post.getString("remark");
        String useGroup = post.getString("useGroup");

        // 可编辑字段
        JSONObject aformData = post.getJSONObject("aformData");
        Record addedRecord = null;
        // 没有或无更新
        if (aformData != null && aformData.size() > 1) {
            try {
                addedRecord = EntityHelper.parse(aformData, getRequestUser(request));
            } catch (DataSpecificationException know) {
                writeFailure(response, know.getLocalizedMessage());
                return;
            }
        }

        try {
            new ApprovalProcessor(recordId)
                    .approve(approver, (ApprovalState) ApprovalState.valueOf(state), remark, selectUsers, addedRecord, useGroup);
            writeSuccess(response);
        } catch (DataSpecificationNoRollbackException ex) {
            writeJSON(response, formatFailure(ex.getMessage(), 499));
        } catch (ApprovalException ex) {
            writeFailure(response, ex.getMessage());
        }
    }

    @RequestMapping("cancel")
    public void doCancel(HttpServletRequest request, HttpServletResponse response) {
        ID recordId = getIdParameterNotNull(request, "record");
        try {
            new ApprovalProcessor(recordId).cancel();
            writeSuccess(response);
        } catch (ApprovalException ex) {
            writeFailure(response, ex.getMessage());
        }
    }

    @RequestMapping("revoke")
    public void doRevoke(HttpServletRequest request, HttpServletResponse response) {
        ID recordId = getIdParameterNotNull(request, "record");
        try {
            new ApprovalProcessor(recordId).revoke();
            writeSuccess(response);
        } catch (ApprovalException ex) {
            writeFailure(response, ex.getMessage());
        }
    }

    @RequestMapping("flow-definition")
    public void getFlowDefinition(HttpServletRequest request, HttpServletResponse response) {
        final ID approvalId = getIdParameterNotNull(request, "id");

        Object[] belongEntity = Application.createQueryNoFilter(
                "select belongEntity from RobotApprovalConfig where configId = ?")
                .setParameter(1, approvalId)
                .unique();
        if (belongEntity == null) {
            writeFailure(response, "无效审批流程，可能已被删除");
            return;
        }

        FlowDefinition def = RobotApprovalManager.instance
                .getFlowDefinition(MetadataHelper.getEntity((String) belongEntity[0]), approvalId);

        JSONObject ret = JSONUtils.toJSONObject(
                new String[]{"applyEntity", "flowDefinition"},
                new Object[]{belongEntity[0], def.getJSON("flowDefinition")});
        writeSuccess(response, ret);
    }

    @RequestMapping("view/{id}")
    public ModelAndView pageView(@PathVariable String id) {
        ModelAndView mv = createModelAndView("/entity/approval/approval-view.jsp");
        mv.getModel().put("approvalId", id);
        return mv;
    }
}
