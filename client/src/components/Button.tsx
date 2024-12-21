type ButtonProps = {
  text: string;
  onClick?: () => void;
};

// generic button style
export default function Button({ text, onClick }: ButtonProps) {
  return (
    <button
      className='relative text-4xl font-courier text-gray-400 font-bold rounded-xl shadow-lg w-1/4 bg-gradient-to-r from-yellow-400 to-pink-400 p-[2px]'
      onClick={onClick}
    >
      <div
        className='h-full w-full bg-black py-6 rounded-xl flex items-center justify-center hover:bg-gray-600'
        aria-hidden='true'
      >
        {text}
      </div>
    </button>
  );
}
