package com.ld.poetry.utils;

import com.ld.poetry.entity.Article;
import com.ld.poetry.entity.Comment;
import com.ld.poetry.entity.User;
import com.ld.poetry.entity.WebInfo;
import com.ld.poetry.im.http.entity.ImChatUserMessage;
import com.ld.poetry.service.CommentService;
import com.ld.poetry.vo.CommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MailSendUtil {

    @Autowired
    private CommonQuery commonQuery;

    @Autowired
    private MailUtil mailUtil;

    public void sendCommentMail(CommentVO commentVO, Article one, CommentService commentService) {
        List<String> mail = new ArrayList<>();
        String toName = "";
        if (commentVO.getParentUserId() != null) {
            User user = commonQuery.getUser(commentVO.getParentUserId());
            if (user != null && !user.getId().equals(PoetryUtil.getUserId()) && StringUtils.hasText(user.getEmail())) {
                toName = user.getUsername();
                mail.add(user.getEmail());
            }
        } else {
            if (one == null) {
                User adminUser = PoetryUtil.getAdminUser();
                if (StringUtils.hasText(adminUser.getEmail()) && !Objects.equals(PoetryUtil.getUserId(), adminUser.getId())) {
                    mail.add(adminUser.getEmail());
                }
            } else {
                User user = commonQuery.getUser(one.getUserId());
                if (user != null && StringUtils.hasText(user.getEmail()) && !user.getId().equals(PoetryUtil.getUserId())) {
                    mail.add(user.getEmail());
                }
            }
        }

        if (!CollectionUtils.isEmpty(mail)) {
            String commentMail = getCommentMail(one == null ? String.valueOf(CommonConst.TREE_HOLE_COMMENT_SOURCE) : one.getArticleTitle(),
                    PoetryUtil.getUsername(),
                    commentVO.getCommentContent(),
                    toName,
                    commentVO.getParentCommentId(), commentService);

            AtomicInteger count = (AtomicInteger) PoetryCache.get(CommonConst.COMMENT_IM_MAIL + mail.get(0));
            if (count == null || count.get() < CommonConst.COMMENT_IM_MAIL_COUNT) {
                WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);
                mailUtil.sendMailMessage(mail, "??????????????????" + (webInfo == null ? "?????????" : webInfo.getWebName()) + "????????????", commentMail);
                if (count == null) {
                    PoetryCache.put(CommonConst.COMMENT_IM_MAIL + mail.get(0), new AtomicInteger(1), CommonConst.TOKEN_EXPIRE * 4);
                } else {
                    count.incrementAndGet();
                }
            }
        }
    }

    /**
     * source???0?????? ?????????????????????
     * fromName????????????
     * toName???????????????
     */
    private String getCommentMail(String source, String fromName, String fromContent, String toName, Integer toCommentId, CommentService commentService) {
        WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);
        String webName = (webInfo == null ? "?????????" : webInfo.getWebName());

        String mailType;
        String toMail = "";
        if (StringUtils.hasText(toName)) {
            mailType = String.format(MailUtil.replyMail, fromName);
            Comment toComment = commentService.lambdaQuery().select(Comment::getCommentContent).eq(Comment::getId, toCommentId).one();
            if (toComment != null) {
                toMail = String.format(MailUtil.originalText, toName, toComment.getCommentContent());
            }
        } else {
            if (String.valueOf(CommonConst.TREE_HOLE_COMMENT_SOURCE).equals(source)) {
                mailType = String.format(MailUtil.messageMail, fromName);
            } else {
                mailType = String.format(MailUtil.commentMail, source, fromName);
            }
        }

        return String.format(MailUtil.mailText,
                webName,
                mailType,
                fromName,
                fromContent,
                toMail,
                webName);
    }

    public void sendImMail(ImChatUserMessage message) {
        if (!message.getMessageStatus()) {
            List<String> mail = new ArrayList<>();
            String username = "";
            User toUser = commonQuery.getUser(message.getToId());
            if (toUser != null && StringUtils.hasText(toUser.getEmail())) {
                mail.add(toUser.getEmail());
            }
            User fromUser = commonQuery.getUser(message.getFromId());
            if (fromUser != null) {
                username = fromUser.getUsername();
            }

            if (!CollectionUtils.isEmpty(mail)) {
                String commentMail = getImMail(username, message.getContent());

                AtomicInteger count = (AtomicInteger) PoetryCache.get(CommonConst.COMMENT_IM_MAIL + mail.get(0));
                if (count == null || count.get() < CommonConst.COMMENT_IM_MAIL_COUNT) {
                    WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);
                    mailUtil.sendMailMessage(mail, "??????????????????" + (webInfo == null ? "?????????" : webInfo.getWebName()) + "????????????", commentMail);
                    if (count == null) {
                        PoetryCache.put(CommonConst.COMMENT_IM_MAIL + mail.get(0), new AtomicInteger(1), CommonConst.TOKEN_EXPIRE * 4);
                    } else {
                        count.incrementAndGet();
                    }
                }
            }
        }
    }

    private String getImMail(String fromName, String fromContent) {
        WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);
        String webName = (webInfo == null ? "?????????" : webInfo.getWebName());

        return String.format(MailUtil.mailText,
                webName,
                String.format(MailUtil.imMail, fromName),
                fromName,
                fromContent,
                "",
                webName);
    }
}
