package gwt.react.todo_mvc.client;

import com.google.gwt.dom.client.InputElement;
import gwt.react.client.api.React;
import gwt.react.client.components.ReactClassSpec;
import gwt.react.client.components.ReactClass;
import gwt.react.client.elements.ReactElement;
import gwt.react.client.events.*;
import gwt.react.client.proptypes.*;
import gwt.react.client.proptypes.html.HtmlProps;
import gwt.react.client.proptypes.html.InputProps;
import gwt.react.client.proptypes.html.attributeTypes.InputType;
import gwt.react.shared.utils.Array;
import gwt.react.client.utils.ObjLiteral;
import gwt.react_router.client.HistoryLocation;
import gwt.react_router.client.RouterEnhancedProps;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

import static gwt.react.client.api.GwtReact.castAsReactElement;
import static gwt.react.client.api.React.DOM.*;
import static gwt.react.client.utils.ObjLiteral.$;

class TodoList extends ReactClassSpec<TodoList.TodoListProps, TodoList.TodoListState> {

    final static String NOW_SHOWING_ACTIVE_TODOS = "active";
    final static String NOW_SHOWING_COMPLETED_TODOS = "completed";

    enum Action { EDIT, DESTROY, TOGGLE, CANCEL }

    @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
    static class TodoRouterParams {
        String nowShowing;
    }

    @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
    static class TodoListProps extends BaseProps implements RouterEnhancedProps<TodoRouterParams>{

        //Having to define the JsProperty annotations again when implementing an interface
        //has been logged as an issue that should be fixed in the future
        @JsProperty(name="location")
        public native HistoryLocation getRouterLocation();

        @JsProperty(name="params")
        public native TodoRouterParams getRouterParams();
    }

    @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
    static class TodoListState extends ObjLiteral {
        String editingId;
        String newTodo;
    }
    
    public TodoListState getInitialState() {
        return $(new TodoListState(), "editingId",null, "newTodo","");
    }

    private void handleDoAction(Action action, TodoModel.Todo todo) {
        switch(action) {
            case TOGGLE :
                App.model.toggle(todo);
                break;
            case CANCEL:
                setState($(new TodoListState(),"editingId", null));
                break;
            case DESTROY:
                App.model.destroy(todo);
                break;
            case EDIT:
                setState($(new TodoListState(),"editingId", todo.id));
        }
    }

    private void handleSave(TodoModel.Todo todoToSave, String text) {
        App.model.save(todoToSave, text);
        setState($(new TodoListState(),"editingId", null));
    }

    private void handleClearCompleted(MouseEvent event) {
        App.model.clearCompleted();
    }

    private void handleToggleAll(FormEvent event) {
        App.model.toggleAll(InputElement.as(event.target).isChecked());
    }

    private void handleNewTodoKeyDown(KeyboardEvent event) {
        if (event.keyCode != App.ENTER_KEY) {
            return;
        }

        event.preventDefault();

        String val = getState().newTodo.trim();

        if (val.length() > 0) {
            App.model.addTodo(val);
            setState($(new TodoListState(), "newTodo", ""));
        }
    }

    private void handleChange(FormEvent event) {
        setState($(new TodoListState(),"newTodo", InputElement.as(event.target).getValue()));
    }

    public ReactElement render() {
        ReactElement footer = null;
        ReactElement main = null;
        Array<TodoModel.Todo> todos = App.model.todos;
        String nowShowing = getProps().getRouterParams().nowShowing;

        Array<TodoModel.Todo> shownTodos = todos.filter((todo, index, theArray) -> {
            if (nowShowing == null) {
                return true;
            }else if (nowShowing.equals(NOW_SHOWING_ACTIVE_TODOS)){
                return !todo.completed;
            }else {
                return todo.completed;
            }
        });

        Array<ReactElement> todoItems = shownTodos.map((todo, index, theArray) -> {
            TodoItem.TodoItemProps todoProps = new TodoItem.TodoItemProps();

            todoProps.key = todo.id;
            todoProps.todo = todo;
            todoProps.doAction = this::handleDoAction;
            todoProps.doSave = this::handleSave;
            todoProps.isEditing = getState().editingId.equals(todo.id);

            return React.createElement(TodoItem.component, todoProps);
        });

        Integer activeTodoCount = todos.reduce((accum, currentValue, index, theArray) ->
                currentValue.completed ? accum : accum + 1, 0);

        int completedCount = todos.length() - activeTodoCount;

        if (activeTodoCount > 0 || completedCount > 0) {
            Footer.FooterProps footerProps = new Footer.FooterProps();
            footerProps.count = activeTodoCount;
            footerProps.completedCount = completedCount;
            footerProps.nowShowing = getProps().getRouterParams().nowShowing;
            footerProps.onClearCompleted = this::handleClearCompleted;

            footer = React.createElement(Footer.component, footerProps);
        }

        if (todos.length() > 0) {
            main = section(new HtmlProps().className("header"),
                        input(new InputProps().className("toggle-all").type(InputType.checkbox).onChange(this::handleToggleAll)),
                        ul(new HtmlProps().className("todo-list"),
                            castAsReactElement(todoItems)
                        )
                    );
        }

        return
            div(null,
                header(new HtmlProps().className("header"),
                        h1(null, "todos"),
                        input(new InputProps()
                                .className("new-todo")
                                .placeHolder("What needs to be done?")
                                .value(getState().newTodo)
                                .onKeyDown(this::handleNewTodoKeyDown)
                                .onChange(this::handleChange)
                                .autoFocus(true))
                ),
                main,
                footer
            );
    }

    public static ReactClass<TodoListProps> component = React.createClass(new TodoList());
}
