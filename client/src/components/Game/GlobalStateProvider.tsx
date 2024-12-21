import React, { createContext, useReducer, useContext, ReactNode } from "react";

export interface Problem {
  name: string;
  description: string;
  signature: Signature;
  tests: Tests;
  problemID: string;
  params: string[];
}

export interface Signature {
  [key: string]: string;
  python: string;
  java: string;
  javascript: string;
}

export interface Tests {
  [key: string]: TestCase;
}

export interface TestCase {
  params: string;
  expected: string;
}

interface Output {
  [key: string]: OutputCase;
}

interface OutputCase {
  expected: string;
  actual: string;
}

type State = {
  roomID: string;
  problem: Problem;
  language: string;
  code: string;
  output: Output;
  stdOutput: string[];
  outputType: string;
  score: string;
};

type Action =
  | { type: "SET_ROOMID"; payload: string }
  | { type: "SET_PROBLEM"; payload: Problem }
  | { type: "SET_LANGUAGE"; payload: string }
  | { type: "SET_CODE"; payload: string }
  | { type: "SET_OUTPUT"; payload: Output }
  | { type: "SET_STD_OUTPUT"; payload: string[] }
  | { type: "SET_OUTPUT_TYPE"; payload: string }
  | { type: "SET_SCORE"; payload: string };

const initialState: State = {
  roomID: "",
  problem: {
    name: "",
    description: "",
    params: [],
    signature: {
      python: "",
      java: "",
      javascript: "",
      "c++": "",
    },
    tests: {
      0: {
        params: "",
        expected: "",
      },
      1: {
        params: "",
        expected: "",
      },
      2: {
        params: "",
        expected: "",
      },
    },
    problemID: "",
  },
  language: "python",
  code: "You did not submit any code!",
  output: {},
  stdOutput: [],
  outputType: "",
  score: "0/10",
};

const GlobalStateContext = createContext<State | undefined>(undefined);
const GlobalDispatchContext = createContext<React.Dispatch<Action> | undefined>(
  undefined
);

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "SET_ROOMID":
      return { ...state, roomID: action.payload };
    case "SET_PROBLEM":
      return { ...state, problem: action.payload };
    case "SET_LANGUAGE":
      return { ...state, language: action.payload };
    case "SET_CODE":
      return { ...state, code: action.payload };
    case "SET_OUTPUT":
      return { ...state, output: action.payload };
    case "SET_STD_OUTPUT":
      return { ...state, stdOutput: action.payload };
    case "SET_OUTPUT_TYPE":
      return { ...state, outputType: action.payload };
    case "SET_SCORE":
      return { ...state, score: action.payload };

    default:
      return state;
  }
}

type GlobalStateProviderProps = {
  children: ReactNode;
};

export const GlobalStateProvider: React.FC<GlobalStateProviderProps> = ({
  children,
}) => {
  const [state, dispatch] = useReducer(reducer, initialState);
  return (
    <GlobalStateContext.Provider value={state}>
      <GlobalDispatchContext.Provider value={dispatch}>
        {children}
      </GlobalDispatchContext.Provider>
    </GlobalStateContext.Provider>
  );
};

export const useGlobalState = (): State => {
  const context = useContext(GlobalStateContext);
  if (!context) {
    throw new Error("useGlobalState must be used within a GlobalStateProvider");
  }
  return context;
};

export const useGlobalDispatch = (): React.Dispatch<Action> => {
  const context = useContext(GlobalDispatchContext);
  if (!context) {
    throw new Error(
      "useGlobalDispatch must be used within a GlobalStateProvider"
    );
  }
  return context;
};
