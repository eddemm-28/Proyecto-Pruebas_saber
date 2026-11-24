package co.edu.unicauca.saberpro.questions.web;

import co.edu.unicauca.saberpro.questions.service.CurrentUser;
import co.edu.unicauca.saberpro.questions.service.QuestionService;
import co.edu.unicauca.saberpro.questions.web.dto.QuestionDtos.PluginGenerateRequest;
import co.edu.unicauca.saberpro.questions.web.dto.QuestionDtos.PluginInfo;
import co.edu.unicauca.saberpro.questions.web.dto.QuestionDtos.QuestionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** A14: Microkernel expuesto por REST; las preguntas generadas se persisten en Borrador. */
@RestController
@RequestMapping("/api/plugins")
public class PluginController {

    private final QuestionService service;

    public PluginController(QuestionService service) {
        this.service = service;
    }

    @GetMapping
    public List<PluginInfo> list() {
        return service.plugins().stream().map(PluginInfo::from).toList();
    }

    @PostMapping("/{pluginId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse generate(@PathVariable String pluginId, @RequestBody PluginGenerateRequest req,
                                     Authentication auth) {
        return QuestionResponse.from(service.generateWithPlugin(pluginId, req.toPluginRequest(), CurrentUser.from(auth)));
    }
}
