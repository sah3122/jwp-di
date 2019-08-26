package next.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import core.annotation.Inject;
import core.annotation.web.Controller;
import core.annotation.web.RequestMapping;
import core.annotation.web.RequestMethod;
import core.mvc.JsonView;
import core.mvc.ModelAndView;
import next.dto.UserCreatedDto;
import next.dto.UserUpdatedDto;
import next.model.User;
import next.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class ApiUserController {

    private static final Logger logger = LoggerFactory.getLogger(ApiUserController.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    private UserRepository userRepository;

    @Inject
    public ApiUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @RequestMapping(value = "/api/users", method = RequestMethod.POST)
    public ModelAndView create(HttpServletRequest request, HttpServletResponse response) throws Exception {
        UserCreatedDto createdDto = objectMapper.readValue(request.getInputStream(), UserCreatedDto.class);
        logger.debug("Created User : {}", createdDto);

        userRepository.insert(new User(
                createdDto.getUserId(),
                createdDto.getPassword(),
                createdDto.getName(),
                createdDto.getEmail()));

        response.setHeader("Location", "/api/users?userId=" + createdDto.getUserId());
        response.setStatus(HttpStatus.CREATED.value());

        return new ModelAndView(new JsonView());
    }

    @RequestMapping(value = "/api/users", method = RequestMethod.GET)
    public ModelAndView show(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String userId = request.getParameter("userId");
        logger.debug("userId : {}", userId);

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("user", userRepository.findByUserId(userId));
        return mav;
    }

    @RequestMapping(value = "/api/users", method = RequestMethod.PUT)
    public ModelAndView update(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String userId = request.getParameter("userId");
        logger.debug("userId : {}", userId);
        UserUpdatedDto updateDto = objectMapper.readValue(request.getInputStream(), UserUpdatedDto.class);
        logger.debug("Updated User : {}", updateDto);

        User user = userRepository.findByUserId(userId);
        user.update(updateDto);
        userRepository.update(user);

        return new ModelAndView(new JsonView());
    }
}
