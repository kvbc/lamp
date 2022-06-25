import json
import ply_parser
import sys

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("[!] No source files given")
    else:
        myparser = ply_parser.build()
        outfn = "lampout"
        json.dump({
            "program": myparser.parse(
                open(sys.argv[1],"r").read()
            )
        }, open(outfn,"w"), indent=2)
        print(f'Output: "{outfn}"')