package hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

@SpringBootApplication
@RestController
public class Application {

    private final Service service = new Service();
    private static final Logger LOGGER
            = LoggerFactory.getLogger(Application.class);
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.initDirectFieldAccess();
  }

  @GetMapping("/")
  public String index() {
    return "Let the battle begin!";
  }

  @PostMapping("/**")
  public String index(@RequestBody Request request) {
      Request.PlayerState self = request.arena.state.get(request._links.self.href);
      LOGGER.info(String.format("location: [%d,%d]", self.x, self.y));
      String command = service.strategy(request).getCommand();
      LOGGER.info(String.format("Command: %s", command));
      return command;
  }

}

