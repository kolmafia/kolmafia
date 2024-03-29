package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class WitchessSolutionDatabaseTest {
  @ParameterizedTest
  @CsvSource({
    "26, witchess.php?sol=0%2C3%7C0%2C5%7C1%2C2%7C1%2C6%7C1%2C8%7C3%2C2%7C3%2C6%7C3%2C8%7C4%2C3%7C4%2C7%7C5%2C4%7C6%2C1%7C6%2C3%7C7%2C0&ajax=1&number=26",
    "36, witchess.php?sol=0%2C1%7C0%2C3%7C0%2C7%7C1%2C0%7C1%2C4%7C1%2C6%7C2%2C1%7C3%2C2%7C3%2C4%7C3%2C6%7C5%2C2%7C5%2C4%7C5%2C6%7C6%2C7%7C7%2C2%7C7%2C4%7C7%2C8%7C8%2C1%7C8%2C5%7C8%2C7&ajax=1&number=36",
    "40, witchess.php?sol=0%2C3%7C0%2C7%7C1%2C2%7C1%2C4%7C1%2C6%7C1%2C8%7C1%2C10%7C2%2C1%7C2%2C9%7C3%2C0%7C3%2C4%7C3%2C6%7C4%2C1%7C4%2C5%7C4%2C9%7C5%2C2%7C5%2C8%7C5%2C10%7C6%2C3%7C6%2C7%7C7%2C4%7C7%2C6%7C7%2C10%7C8%2C1%7C8%2C5%7C8%2C9%7C9%2C0%7C9%2C2%7C9%2C8%7C10%2C3%7C10%2C5%7C10%2C7&ajax=1&number=40",
    "49, witchess.php?sol=0%2C5%7C1%2C4%7C1%2C6%7C1%2C10%7C2%2C3%7C3%2C2%7C3%2C6%7C3%2C10%7C4%2C7%7C4%2C9%7C5%2C2%7C7%2C2%7C8%2C3%7C8%2C5%7C9%2C6%7C10%2C1%7C10%2C3%7C10%2C5&ajax=1&number=49",
    "51, witchess.php?sol=0%2C5%7C0%2C9%7C0%2C11%7C1%2C4%7C1%2C6%7C1%2C8%7C3%2C4%7C3%2C6%7C3%2C8%7C4%2C1%7C4%2C3%7C4%2C9%7C4%2C11%7C5%2C0%7C5%2C6%7C5%2C12%7C7%2C0%7C7%2C6%7C7%2C12%7C8%2C1%7C8%2C3%7C8%2C9%7C8%2C11%7C9%2C4%7C9%2C6%7C9%2C8%7C11%2C4%7C11%2C6%7C11%2C8%7C12%2C1%7C12%2C3%7C12%2C7&ajax=1&number=51",
    "60, witchess.php?sol=0%2C5%7C0%2C7%7C0%2C9%7C0%2C11%7C1%2C4%7C1%2C12%7C1%2C16%7C2%2C9%7C2%2C13%7C3%2C4%7C3%2C8%7C3%2C10%7C3%2C14%7C3%2C16%7C4%2C5%7C4%2C7%7C5%2C10%7C5%2C14%7C5%2C16%7C6%2C11%7C6%2C15%7C7%2C12%7C8%2C5%7C8%2C7%7C8%2C9%7C8%2C13%7C9%2C4%7C9%2C10%7C9%2C14%7C10%2C1%7C10%2C3%7C10%2C7%7C11%2C0%7C11%2C6%7C11%2C8%7C11%2C10%7C11%2C14%7C12%2C9%7C13%2C0%7C13%2C6%7C13%2C14%7C14%2C5%7C14%2C13%7C15%2C0%7C15%2C4%7C15%2C12%7C16%2C5%7C16%2C7%7C16%2C9%7C16%2C11&ajax=1&number=60",
    "69, witchess.php?sol=1%2C10%7C2%2C1%7C2%2C3%7C2%2C5%7C2%2C7%7C2%2C9%7C3%2C0%7C4%2C3%7C4%2C5%7C4%2C7%7C4%2C9%7C5%2C0%7C5%2C2%7C5%2C10%7C6%2C7%7C6%2C9%7C7%2C0%7C7%2C2%7C7%2C6%7C8%2C1%7C8%2C5%7C9%2C4%7C10%2C1%7C10%2C3&ajax=1&number=69",
    "122, witchess.php?sol=0%2C1%7C0%2C9%7C1%2C0%7C1%2C2%7C1%2C8%7C2%2C9%7C3%2C0%7C3%2C2%7C3%2C10%7C5%2C0%7C5%2C2%7C5%2C10%7C6%2C3%7C6%2C7%7C6%2C9%7C7%2C0%7C7%2C4%7C7%2C6%7C8%2C3%7C9%2C0%7C9%2C2%7C9%2C6%7C10%2C3%7C11%2C0%7C11%2C4%7C11%2C6%7C12%2C1%7C13%2C2%7C13%2C4%7C13%2C6%7C14%2C7%7C14%2C9%7C15%2C2%7C15%2C4%7C15%2C10%7C17%2C2%7C17%2C4%7C17%2C10%7C18%2C7%7C18%2C9%7C19%2C2%7C19%2C4%7C19%2C6%7C20%2C5%7C21%2C2%7C22%2C3%7C22%2C5%7C22%2C7%7C22%2C9%7C23%2C10%7C24%2C1%7C24%2C3%7C24%2C5%7C25%2C0%7C25%2C6%7C25%2C10%7C26%2C3%7C26%2C5%7C27%2C0%7C27%2C2%7C27%2C10%7C28%2C3%7C28%2C5%7C28%2C7%7C28%2C9&ajax=1&number=122",
    "141, witchess.php?sol=0%2C9%7C1%2C8%7C1%2C10%7C1%2C18%7C3%2C8%7C3%2C10%7C3%2C18%7C5%2C8%7C5%2C10%7C5%2C18%7C6%2C1%7C6%2C3%7C6%2C5%7C6%2C7%7C6%2C11%7C6%2C13%7C6%2C15%7C6%2C17%7C7%2C0%7C8%2C1%7C8%2C3%7C8%2C5%7C8%2C7%7C8%2C9%7C8%2C11%7C9%2C12%7C10%2C3%7C10%2C7%7C10%2C9%7C10%2C13%7C10%2C15%7C10%2C17%7C11%2C2%7C11%2C4%7C11%2C6%7C11%2C10%7C11%2C18%7C12%2C13%7C12%2C15%7C12%2C17%7C13%2C2%7C13%2C4%7C13%2C6%7C13%2C10%7C13%2C12%7C14%2C1%7C15%2C0%7C15%2C4%7C15%2C6%7C15%2C10%7C15%2C12%7C17%2C0%7C17%2C4%7C17%2C6%7C17%2C10%7C17%2C12%7C18%2C5%7C18%2C11&ajax=1&number=141",
    "150, witchess.php?sol=0%2C1%7C0%2C3%7C0%2C5%7C0%2C7%7C0%2C9%7C0%2C11%7C0%2C13%7C0%2C15%7C0%2C17%7C0%2C19%7C0%2C21%7C0%2C23%7C0%2C25%7C0%2C27%7C0%2C29%7C0%2C31%7C0%2C33%7C0%2C35%7C0%2C37%7C0%2C39%7C0%2C41%7C0%2C43%7C0%2C45%7C0%2C47%7C0%2C49%7C0%2C51%7C0%2C53%7C0%2C55%7C0%2C57%7C0%2C59%7C0%2C61%7C0%2C63%7C0%2C65%7C0%2C67%7C0%2C69%7C0%2C71%7C0%2C73%7C1%2C0%7C3%2C0%7C4%2C1%7C4%2C3%7C4%2C5%7C4%2C13%7C4%2C15%7C4%2C17%7C4%2C19%7C4%2C21%7C4%2C23%7C4%2C25%7C4%2C27%7C4%2C29%7C4%2C31%7C4%2C33%7C4%2C35%7C4%2C37%7C4%2C39%7C4%2C41%7C4%2C45%7C4%2C47%7C4%2C49%7C4%2C51%7C4%2C53%7C4%2C55%7C4%2C57%7C4%2C59%7C4%2C61%7C4%2C63%7C4%2C65%7C4%2C67%7C4%2C69%7C5%2C6%7C5%2C12%7C5%2C42%7C5%2C44%7C5%2C70%7C6%2C1%7C6%2C3%7C6%2C15%7C6%2C17%7C6%2C21%7C6%2C23%7C6%2C25%7C6%2C29%7C6%2C31%7C6%2C35%7C6%2C37%7C6%2C39%7C6%2C47%7C6%2C51%7C6%2C53%7C6%2C55%7C6%2C59%7C6%2C61%7C6%2C63%7C6%2C67%7C6%2C69%7C7%2C0%7C7%2C4%7C7%2C6%7C7%2C12%7C7%2C14%7C7%2C18%7C7%2C20%7C7%2C26%7C7%2C28%7C7%2C32%7C7%2C34%7C7%2C40%7C7%2C42%7C7%2C44%7C7%2C46%7C7%2C48%7C7%2C50%7C7%2C56%7C7%2C58%7C7%2C64%7C7%2C66%7C8%2C43%7C8%2C51%7C8%2C59%7C8%2C61%7C8%2C67%7C8%2C69%7C9%2C0%7C9%2C4%7C9%2C6%7C9%2C12%7C9%2C14%7C9%2C18%7C9%2C20%7C9%2C26%7C9%2C28%7C9%2C32%7C9%2C34%7C9%2C40%7C9%2C46%7C9%2C48%7C9%2C52%7C9%2C56%7C9%2C62%7C9%2C64%7C9%2C70%7C10%2C9%7C10%2C43%7C10%2C51%7C10%2C57%7C10%2C59%7C10%2C65%7C10%2C67%7C11%2C0%7C11%2C4%7C11%2C6%7C11%2C8%7C11%2C10%7C11%2C12%7C11%2C14%7C11%2C18%7C11%2C20%7C11%2C26%7C11%2C28%7C11%2C32%7C11%2C34%7C11%2C40%7C11%2C42%7C11%2C44%7C11%2C46%7C11%2C48%7C11%2C50%7C11%2C60%7C11%2C62%7C11%2C68%7C11%2C70%7C12%2C7%7C12%2C11%7C12%2C17%7C12%2C21%7C12%2C35%7C12%2C37%7C12%2C51%7C12%2C53%7C12%2C57%7C12%2C59%7C12%2C65%7C12%2C67%7C13%2C0%7C13%2C4%7C13%2C14%7C13%2C16%7C13%2C22%7C13%2C26%7C13%2C28%7C13%2C32%7C13%2C38%7C13%2C40%7C13%2C42%7C13%2C44%7C13%2C46%7C13%2C48%7C13%2C54%7C13%2C56%7C13%2C62%7C13%2C64%7C13%2C70%7C14%2C5%7C14%2C7%7C14%2C9%7C14%2C11%7C14%2C13%7C14%2C17%7C14%2C19%7C14%2C21%7C14%2C27%7C14%2C33%7C14%2C35%7C14%2C37%7C14%2C41%7C14%2C45%7C14%2C49%7C14%2C51%7C14%2C53%7C14%2C57%7C14%2C59%7C14%2C61%7C14%2C65%7C14%2C67%7C14%2C69%7C15%2C0%7C17%2C0&ajax=1&number=150"
  })
  public void solutionIsCorrect(int puzzleId, String expectedSolution) {
    var sol = WitchessSolutionDatabase.getWitchessSolution(puzzleId);
    var req = new GenericRequest("witchess.php");
    req.addFormField("sol", sol.coords);
    req.addFormField("ajax", "1");
    req.addFormField("number", String.valueOf(puzzleId));

    var reqUrl = req.getURLString();

    assertThat(reqUrl, is(expectedSolution));
  }
}
