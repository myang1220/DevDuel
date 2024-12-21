import { useRef, useState } from "react";
import { Editor } from "@monaco-editor/react";
import LanguageSelector from "./LanguageSelector";
import { Language, LANGUAGE_VERSIONS } from "../constants";
import { Button } from "@chakra-ui/react";
import { useGlobalDispatch, useGlobalState } from "../GlobalStateProvider";
import { queryAPIPost } from "../../../api.ts";
import { Store } from "react-notifications-component";
import "react-notifications-component/dist/theme.css";
import { queryAPI } from "../../../api";
import { useUser } from "@clerk/clerk-react";

// contains logic for code editor, including run and submit
export default function CodeEditor({
  onFinish,
}: {
  onFinish: (win: boolean) => void;
}) {
  const editorRef = useRef();
  const [value, setValue] = useState("");
  const [tempScore, setTempScore] = useState("");
  const state = useGlobalState();
  const dispatch = useGlobalDispatch();
  const user = useUser().user;
  let username = user?.username
    ? user.username
    : user?.emailAddresses
        ?.find((email) => email.id === user.primaryEmailAddressId)
        ?.emailAddress?.split("@")[0];

  if (!username) {
    username = localStorage.getItem("guestName") || "";
  }

  // set up the editor
  function onMount(editor: { focus: () => void } | undefined) {
    editorRef.current = editor as any;
    if (editor) {
      editor.focus();
    }
  }

  // set the language of the editor
  function onSelect(language: Language) {
    dispatch({ type: "SET_LANGUAGE", payload: language });
    setValue(state.problem.signature[language]);
  }

  // use runcode endpoint to run code, show notification for result
  async function runCode() {
    const body = {
      language: state.language,
      version: LANGUAGE_VERSIONS[state.language],
      name: state.problem.name,
      code: value,
    };
    const response = await queryAPIPost("runcode", body);

    if (response.response_type === "success") {
      // show a success notification with number of cases passed
      dispatch({ type: "SET_OUTPUT", payload: response.tests });
      dispatch({ type: "SET_STD_OUTPUT", payload: response.output });
      dispatch({ type: "SET_OUTPUT_TYPE", payload: "success" });
      setTempScore(response.score);
      Store.addNotification({
        title: "Success!",
        message: `You scored ${response.score} points! Press submit to submit your score!`,
        type: "success",
        insert: "top",
        container: "top-center",
        animationIn: ["animate__animated", "animate__fadeIn"],
        animationOut: ["animate__animated", "animate__fadeOut"],
        dismiss: {
          duration: 2000,
          onScreen: true,
        },
      });
    } else if (response.response_type === "bug") {
      // show an error notification
      dispatch({ type: "SET_OUTPUT_TYPE", payload: "bug" });
      dispatch({ type: "SET_STD_OUTPUT", payload: response.output });
      Store.addNotification({
        title: "Error!",
        message: `There's an error with your code!`,
        type: "danger",
        insert: "top",
        container: "top-center",
        animationIn: ["animate__animated", "animate__fadeIn"],
        animationOut: ["animate__animated", "animate__fadeOut"],
        dismiss: {
          duration: 2000,
          onScreen: true,
        },
      });
    } else if (response.response_type === "failure") {
      // show a failure notification
      dispatch({ type: "SET_OUTPUT_TYPE", payload: "failure" });
      Store.addNotification({
        title: "Failure!",
        message: `${response.error}`,
        type: "danger",
        insert: "top",
        container: "top-center",
        animationIn: ["animate__animated", "animate__fadeIn"],
        animationOut: ["animate__animated", "animate__fadeOut"],
        dismiss: {
          duration: 2000,
          onScreen: true,
        },
      });
    }

    return response.data;
  }

  // helper to convert string fraction to number
  const strFracToNum = (score: string) => {
    const parts = score.split("/");
    return parseInt(parts[0], 10) / parseInt(parts[1], 10);
  };

  // use RoomSet endpoint to submit score
  async function submitScore() {
    if (strFracToNum(tempScore) >= strFracToNum(state.score)) {
      await queryAPI("RoomSet", {
        roomID: state.roomID,
        userName: username as string,
        userID: user?.id || username || "",
        userScore: tempScore,
        timeSubmitted: new Date().toISOString(),
      });
      dispatch({ type: "SET_CODE", payload: value });
      dispatch({ type: "SET_SCORE", payload: tempScore });
    } else {
      // if the score is less than the current score, show failure notification
      Store.addNotification({
        title: "Failure!",
        message: `You scored ${tempScore} points, which is less than your current score of ${state.score} points!`,
        type: "danger",
        insert: "top",
        container: "top-center",
        animationIn: ["animate__animated", "animate__fadeIn"],
        animationOut: ["animate__animated", "animate__fadeOut"],
        dismiss: {
          duration: 2000,
          onScreen: true,
        },
      });
    }
    // finish game if you get all test cases
    if (strFracToNum(tempScore) === 1) {
      onFinish(true);
    }
  }

  if (!state.problem.signature[state.language]) {
    return <div>Loading problem...</div>;
  }

  return (
    <div className="flex flex-col h-full overflow-hidden py-2">
      <LanguageSelector
        language={state.language as Language}
        onSelect={onSelect}
      />
      <div className="overflow-hidden h-full">
        <Editor
          options={{
            padding: {
              top: 10,
              bottom: 10,
            },
            minimap: {
              enabled: false,
            },
            scrollBeyondLastLine: false,
          }}
          theme="vs-dark"
          language={state.language}
          defaultValue={state.problem.signature[state.language]
            .replace(/\\n/g, "\n")
            .replace(/\\t/g, "\t")}
          onMount={onMount}
          value={value.replace(/\\n/g, "\n").replace(/\\t/g, "\t")}
          onChange={(value) => value !== undefined && setValue(value)}
          className="editor-wrapper"
        />
      </div>
      <div className="flex flex-row gap-2 justify-end pr-4">
        <Button size={"sm"} paddingX={4} onClick={runCode}>
          Run
        </Button>
        <Button
          size={"sm"}
          paddingX={4}
          bg={"#2DBB5D"}
          _hover={{ bg: "#4DC576" }}
          onClick={submitScore}
        >
          Submit
        </Button>
      </div>
    </div>
  );
}
